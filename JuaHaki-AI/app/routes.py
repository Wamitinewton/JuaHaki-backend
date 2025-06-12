from flask import Blueprint, request, jsonify, current_app
from datetime import datetime
import logging
import time
from .exceptions import ChatbotError, ValidationError

logger = logging.getLogger(__name__)

api_bp = Blueprint('api', __name__)

def get_chatbot():
    """Get the chatbot instance from app config."""
    chatbot = current_app.config.get('CHATBOT')
    if not chatbot or not chatbot.is_ready:
        error_msg = current_app.config.get('CHATBOT_ERROR', 'Chatbot service is not available')
        raise ChatbotError(error_msg)
    return chatbot

def validate_ask_request(data):
    """Validate the ask endpoint request data."""
    if not data:
        raise ValidationError("No data provided")
    
    question = data.get('question', '').strip()
    if not question:
        raise ValidationError("Question is required and cannot be empty")
    
    max_length = current_app.config.get('MAX_QUESTION_LENGTH', 1000)
    if len(question) > max_length:
        raise ValidationError(f"Question is too long (max {max_length} characters)")
    
    top_k = data.get('top_k', current_app.config.get('DEFAULT_TOP_K', 5))
    max_top_k = current_app.config.get('MAX_TOP_K', 20)
    
    if not isinstance(top_k, int) or top_k < 1 or top_k > max_top_k:
        top_k = current_app.config.get('DEFAULT_TOP_K', 5)
    
    doc_filter = data.get('doc_filter')
    valid_filters = ['constitution', 'ten_years_assessment', 'human_rights_essays']
    
    if doc_filter and doc_filter not in valid_filters:
        raise ValidationError(f"Invalid doc_filter. Must be one of: {', '.join(valid_filters)}")
    
    return {
        'question': question,
        'top_k': top_k,
        'doc_filter': doc_filter
    }

def create_success_response(data, message="Request processed successfully"):
    """Create a standardized success response."""
    return {
        'success': True,
        'error': None,
        'message': message,
        'data': data
    }

def log_request(endpoint, question=None, processing_time=None):
    """Log request information."""
    log_msg = f"API {endpoint} - "
    if question:
        log_msg += f"Question: {question[:50]}{'...' if len(question) > 50 else ''} - "
    if processing_time:
        log_msg += f"Processing time: {processing_time:.2f}s"
    
    logger.info(log_msg)

@api_bp.route('/ask', methods=['POST'])
def ask_question():
    """
    Main endpoint for asking questions to the chatbot.
    
    Expected JSON payload:
    {
        "question": "Your question here",
        "top_k": 5,  # optional, default from config
        "doc_filter": null  # optional, filter by document type
    }
    
    Returns:
    {
        "success": true,
        "error": null,
        "message": "Question processed successfully",
        "data": {
            "answer": "...",
            "question": "...",
            "parameters": {...},
            "metadata": {...}
        }
    }
    """
    start_time = time.time()
    
    try:
        if not request.is_json:
            raise ValidationError("Request must be JSON")
        
        data = request.get_json()
        validated_data = validate_ask_request(data)
        
        chatbot = get_chatbot()
        
        question = validated_data['question']
        top_k = validated_data['top_k']
        doc_filter = validated_data['doc_filter']
        
        logger.info(f"Processing question: {question[:100]}...")
        
        response = chatbot.ask(
            question=question,
            top_k=top_k,
            doc_filter=doc_filter
        )
        
        processing_time = time.time() - start_time
        
        response_data = {
            'answer': response,
            'question': question,
            'parameters': {
                'top_k': top_k,
                'doc_filter': doc_filter
            },
            'metadata': {
                'processing_time': round(processing_time, 2),
                'timestamp': datetime.utcnow().isoformat(),
                'model_info': {
                    'embeddings': 'cohere',
                    'generation': 'gemini-1.5-flash',
                    'response_generation': 'command-r-plus'
                }
            }
        }
        
        log_request('ask', question, processing_time)
        
        return jsonify(create_success_response(
            response_data, 
            "Question processed successfully"
        )), 200
        
    except ValidationError as e:
        logger.warning(f"Validation error in ask endpoint: {str(e)}")
        raise e
    except ChatbotError as e:
        logger.error(f"Chatbot error in ask endpoint: {str(e)}")
        raise e
    except Exception as e:
        logger.error(f"Unexpected error in ask endpoint: {str(e)}")
        raise ChatbotError(f"Failed to process question: {str(e)}")

@api_bp.route('/stats', methods=['GET'])
def get_document_stats():
    """
    Get document statistics.
    
    Returns:
    {
        "success": true,
        "error": null,
        "message": "Statistics retrieved successfully",
        "data": {
            "documents": {...},
            "total_chunks": int,
            "available_filters": [...]
        }
    }
    """
    try:
        chatbot = get_chatbot()
        
        stats = chatbot.retriever.get_document_stats()
        
        formatted_stats = {}
        for doc_type, count in stats.items():
            doc_name = chatbot.retriever.document_types.get(doc_type, doc_type)
            formatted_stats[doc_type] = {
                'name': doc_name,
                'chunks': count
            }
        
        total_chunks = sum(stats.values())
        
        response_data = {
            'documents': formatted_stats,
            'total_chunks': total_chunks,
            'available_filters': list(stats.keys()),
            'metadata': {
                'timestamp': datetime.utcnow().isoformat()
            }
        }
        
        log_request('stats')
        
        return jsonify(create_success_response(
            response_data,
            "Statistics retrieved successfully"
        )), 200
        
    except ChatbotError as e:
        logger.error(f"Chatbot error in stats endpoint: {str(e)}")
        raise e
    except Exception as e:
        logger.error(f"Unexpected error in stats endpoint: {str(e)}")
        raise ChatbotError(f"Failed to retrieve statistics: {str(e)}")

@api_bp.route('/health', methods=['GET'])
def api_health():
    """
    API-specific health check endpoint.
    
    Returns:
    {
        "success": true,
        "error": null,
        "message": "API is healthy",
        "data": {
            "status": "healthy",
            "chatbot_ready": bool,
            "api_version": "1.0.0"
        }
    }
    """
    try:
        chatbot = current_app.config.get('CHATBOT')
        chatbot_ready = chatbot is not None and chatbot.is_ready
        
        response_data = {
            'status': 'healthy' if chatbot_ready else 'degraded',
            'chatbot_ready': chatbot_ready,
            'api_version': '1.0.0',
            'timestamp': datetime.utcnow().isoformat()
        }
        
        if not chatbot_ready:
            error_msg = current_app.config.get('CHATBOT_ERROR', 'Chatbot not ready')
            response_data['chatbot_error'] = error_msg
        
        status_code = 200 if chatbot_ready else 503
        message = "API is healthy" if chatbot_ready else "API is degraded - chatbot not ready"
        
        return jsonify(create_success_response(response_data, message)), status_code
        
    except Exception as e:
        logger.error(f"Error in API health check: {str(e)}")
        return jsonify({
            'success': False,
            'error': 'health_check_error',
            'message': str(e),
            'data': None
        }), 500

@api_bp.route('/info', methods=['GET'])
def api_info():
    """
    Get API information and available endpoints.
    
    Returns information about the API, including available endpoints,
    expected parameters, and usage examples.
    """
    info_data = {
        'service': 'Civil Rights Chatbot API',
        'version': '1.0.0',
        'description': 'AI-powered legal assistant for Kenyan civil rights and constitutional law',
        'endpoints': {
            '/api/ask': {
                'method': 'POST',
                'description': 'Ask questions about civil rights and constitutional law',
                'parameters': {
                    'question': {'type': 'string', 'required': True, 'description': 'Your legal question'},
                    'top_k': {'type': 'int', 'required': False, 'default': 5, 'description': 'Number of documents to retrieve'},
                    'doc_filter': {'type': 'string', 'required': False, 'options': ['constitution', 'ten_years_assessment', 'human_rights_essays']}
                }
            },
            '/api/stats': {
                'method': 'GET',
                'description': 'Get document statistics',
                'parameters': None
            },
            '/api/health': {
                'method': 'GET',
                'description': 'Check API health status',
                'parameters': None
            }
        },
        'example_request': {
            'url': '/api/ask',
            'method': 'POST',
            'headers': {'Content-Type': 'application/json'},
            'body': {
                'question': 'What are the fundamental rights in the Kenyan constitution?',
                'top_k': 5
            }
        },
        'timestamp': datetime.utcnow().isoformat()
    }
    
    return jsonify(create_success_response(
        info_data,
        "API information retrieved successfully"
    )), 200