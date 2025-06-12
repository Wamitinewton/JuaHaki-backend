from flask import Flask
from flask_cors import CORS
import logging
import os
from .exceptions import ChatbotError, ValidationError
from .routes import api_bp
from .chatbot import CivilRightsChatbot

logger = logging.getLogger(__name__)

chatbot = None

def create_app(config_class=None):
    """Application factory function."""
    app = Flask(__name__)
    
    if config_class:
        app.config.from_object(config_class)
    else:
        from config import DevelopmentConfig
        app.config.from_object(DevelopmentConfig)
    
    CORS(app, resources={
        r"/api/*": {
            "origins": ["http://localhost:3000", "http://127.0.0.1:3000"],
            "methods": ["GET", "POST", "OPTIONS"],
            "allow_headers": ["Content-Type", "Authorization"]
        }
    })
    
    initialize_chatbot(app)
    
    app.register_blueprint(api_bp, url_prefix='/api')
    
    register_error_handlers(app)
    
    @app.route('/health')
    def health_check():
        from flask import jsonify
        from datetime import datetime
        
        global chatbot
        
        status = {
            'service': 'Civil Rights Chatbot API',
            'version': '1.0.0',
            'status': 'healthy',
            'chatbot_ready': chatbot is not None and chatbot.is_ready,
            'timestamp': datetime.utcnow().isoformat()
        }
        
        if not status['chatbot_ready']:
            status['status'] = 'degraded'
            return jsonify(status), 503
        
        return jsonify(status), 200
    
    logger.info("Flask application created successfully")
    return app

def initialize_chatbot(app):
    """Initialize the global chatbot instance."""
    global chatbot
    
    try:
        logger.info("Initializing Civil Rights Chatbot...")
        
        # Check requirements
        if not check_requirements(app.config):
            raise ChatbotError("Requirements check failed")
        
        # Initialize chatbot
        embeddings_dir = str(app.config.get('EMBEDDINGS_DIR', 'embeddings'))
        chatbot = CivilRightsChatbot(embeddings_dir)
        
        pdf_paths = app.config.get('PDF_PATHS', {})
        if not chatbot.initialize(pdf_paths):
            raise ChatbotError("Failed to initialize chatbot with documents")
        
        # Store chatbot in app config for access in routes
        app.config['CHATBOT'] = chatbot
        
        logger.info("Chatbot initialized successfully")
        
    except Exception as e:
        logger.error(f"Chatbot initialization error: {str(e)}")
        app.config['CHATBOT'] = None
        app.config['CHATBOT_ERROR'] = str(e)

def check_requirements(config):
    """Check if required files and environment variables exist."""
    pdf_paths = config.get('PDF_PATHS', {})
    missing_files = []
    
    for doc_type, pdf_path in pdf_paths.items():
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

def register_error_handlers(app):
    """Register global error handlers."""
    from flask import jsonify
    
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

    @app.errorhandler(429)
    def handle_rate_limit(error):
        """Handle rate limit errors."""
        return jsonify({
            'success': False,
            'error': 'rate_limit_exceeded',
            'message': 'Too many requests. Please try again later.',
            'data': None
        }), 429