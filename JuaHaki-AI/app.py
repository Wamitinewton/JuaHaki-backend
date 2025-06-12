from flask import Flask, request, jsonify
from flask_cors import CORS
import logging
import os
from pathlib import Path
from app.chatbot import CivilRightsChatbot
from app.exceptions import ChatbotError, ValidationError
from dotenv import load_dotenv

load_dotenv()

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

app = Flask(__name__)
CORS(app)

PDF_PATHS = {
    'constitution': "data/Constitution.pdf",
    'ten_years_assessment': "data/Ten_Years_Assessment.pdf",
    'human_rights_essays': "data/Understanding_Human_Rights.pdf"
}
EMBEDDINGS_DIR = "embeddings"

chatbot = None

def initialize_chatbot():
    """Initialize the chatbot with error handling."""
    global chatbot
    try:
        logger.info("Initializing Civil Rights Chatbot...")
        chatbot = CivilRightsChatbot(EMBEDDINGS_DIR)
        
        if not chatbot.initialize(PDF_PATHS):
            raise ChatbotError("Failed to initialize chatbot with documents")
        
        logger.info("Chatbot initialized successfully")
        return True
    except Exception as e:
        logger.error(f"Chatbot initialization error: {str(e)}")
        return False

@app.errorhandler(ValidationError)
def handle_validation_error(error):
    """Handle validation errors."""
    return jsonify({
        'success': False,
        'error': 'validation_error',
        'message': str(error),
        'data': None
    }), 400

@app.errorhandler(ChatbotError)
def handle_chatbot_error(error):
    """Handle chatbot-specific errors."""
    return jsonify({
        'success': False,
        'error': 'chatbot_error',
        'message': str(error),
        'data': None
    }), 500

@app.errorhandler(500)
def handle_internal_error(error):
    """Handle internal server errors."""
    logger.error(f"Internal server error: {str(error)}")
    return jsonify({
        'success': False,
        'error': 'internal_error',
        'message': 'An internal server error occurred',
        'data': None
    }), 500

@app.errorhandler(404)
def handle_not_found(error):
    """Handle 404 errors."""
    return jsonify({
        'success': False,
        'error': 'not_found',
        'message': 'Endpoint not found',
        'data': None
    }), 404

@app.route('/health', methods=['GET'])
def health_check():
    """Health check endpoint."""
    global chatbot
    
    status = {
        'service': 'Civil Rights Chatbot API',
        'version': '1.0.0',
        'status': 'healthy',
        'chatbot_ready': chatbot is not None and chatbot.is_ready,
        'timestamp': None
    }
    
    from datetime import datetime
    status['timestamp'] = datetime.utcnow().isoformat()
    
    if not status['chatbot_ready']:
        status['status'] = 'degraded'
        return jsonify(status), 503
    
    return jsonify(status), 200

@app.route('/ask', methods=['POST'])
def ask_question():
    """
    Main endpoint for asking questions to the chatbot.
    
    Expected JSON payload:
    {
        "question": "Your question here",
        "top_k": 5,  # optional, default 5
        "doc_filter": null  # optional, filter by document type
    }
    """
    global chatbot
    
    try:
        if not chatbot or not chatbot.is_ready:
            raise ChatbotError("Chatbot service is not available")
        
        if not request.is_json:
            raise ValidationError("Request must be JSON")
        
        data = request.get_json()
        
        if not data:
            raise ValidationError("No data provided")
        
        question = data.get('question', '').strip()
        if not question:
            raise ValidationError("Question is required and cannot be empty")
        
        if len(question) > 1000:
            raise ValidationError("Question is too long (max 1000 characters)")
        
        top_k = data.get('top_k', 5)
        doc_filter = data.get('doc_filter')
        
        if not isinstance(top_k, int) or top_k < 1 or top_k > 20:
            top_k = 5
        
        valid_filters = ['constitution', 'ten_years_assessment', 'human_rights_essays']
        if doc_filter and doc_filter not in valid_filters:
            raise ValidationError(f"Invalid doc_filter. Must be one of: {', '.join(valid_filters)}")
        
        logger.info(f"Processing question: {question[:100]}...")
        
        response = chatbot.ask(
            question=question,
            top_k=top_k,
            doc_filter=doc_filter
        )
        
        response_data = {
            'answer': response,
            'question': question,
            'parameters': {
                'top_k': top_k,
                'doc_filter': doc_filter
            },
            'metadata': {
                'processing_time': None,
                'timestamp': None
            }
        }
        
        from datetime import datetime
        response_data['metadata']['timestamp'] = datetime.utcnow().isoformat()
        
        logger.info("Question processed successfully")
        
        return jsonify({
            'success': True,
            'error': None,
            'message': 'Question processed successfully',
            'data': response_data
        }), 200
        
    except ValidationError as e:
        logger.warning(f"Validation error: {str(e)}")
        raise e
    except ChatbotError as e:
        logger.error(f"Chatbot error: {str(e)}")
        raise e
    except Exception as e:
        logger.error(f"Unexpected error in ask endpoint: {str(e)}")
        raise ChatbotError(f"Failed to process question: {str(e)}")

@app.route('/stats', methods=['GET'])
def get_stats():
    """Get document statistics."""
    global chatbot
    
    try:
        if not chatbot or not chatbot.is_ready:
            raise ChatbotError("Chatbot service is not available")
        
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
            'available_filters': list(stats.keys())
        }
        
        return jsonify({
            'success': True,
            'error': None,
            'message': 'Statistics retrieved successfully',
            'data': response_data
        }), 200
        
    except ChatbotError as e:
        logger.error(f"Stats error: {str(e)}")
        raise e
    except Exception as e:
        logger.error(f"Unexpected error in stats endpoint: {str(e)}")
        raise ChatbotError(f"Failed to retrieve statistics: {str(e)}")

def check_requirements():
    """Check if required files and environment variables exist."""
    missing_files = []
    
    for doc_type, pdf_path in PDF_PATHS.items():
        if not os.path.exists(pdf_path):
            missing_files.append(f"{pdf_path} ({doc_type})")
    
    if missing_files:
        logger.error("Missing PDF files:")
        for file in missing_files:
            logger.error(f"  - {file}")
        return False
    
    if not os.path.exists('.env'):
        logger.error(".env file not found")
        return False
    
    if not os.getenv('COHERE_API_KEY'):
        logger.error("COHERE_API_KEY not found in .env file")
        return False
    
    if not os.getenv('GEMINI_API_KEY'):
        logger.error("GEMINI_API_KEY not found in .env file")
        return False
    
    return True

if __name__ == '__main__':
    if not check_requirements():
        logger.error("Requirements check failed. Exiting.")
        exit(1)
    
    if not initialize_chatbot():
        logger.error("Failed to initialize chatbot. Exiting.")
        exit(1)
    
    port = int(os.getenv('PORT', 5000))
    debug = os.getenv('FLASK_ENV') == 'development'
    
    logger.info(f"Starting Civil Rights Chatbot API on port {port}")
    app.run(host='0.0.0.0', port=port, debug=debug)