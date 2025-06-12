import os
import sys
import logging
from pathlib import Path

project_root = Path(__file__).parent
sys.path.insert(0, str(project_root))

from app import create_app
from config import config

def setup_logging():
    """Configure logging for the application."""
    log_level = os.getenv('LOG_LEVEL', 'INFO').upper()
    logging.basicConfig(
        level=getattr(logging, log_level),
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
        handlers=[
            logging.StreamHandler(sys.stdout),
            logging.FileHandler('app.log') if os.getenv('LOG_FILE') else logging.NullHandler()
        ]
    )

def main():
    """Main application entry point."""
    setup_logging()
    logger = logging.getLogger(__name__)
    
    config_name = os.getenv('FLASK_ENV', 'development')
    app_config = config.get(config_name, config['default'])
    
    try:
        app = create_app(app_config)
        
        host = os.getenv('HOST', '0.0.0.0')
        port = int(os.getenv('PORT', 5000))
        debug = app_config.DEBUG
        
        logger.info(f"Starting Civil Rights Chatbot API")
        logger.info(f"Environment: {config_name}")
        logger.info(f"Debug mode: {debug}")
        logger.info(f"Server: http://{host}:{port}")
        
        app.run(host=host, port=port, debug=debug, threaded=True)
        
    except KeyboardInterrupt:
        logger.info("Server shutdown requested by user")
    except Exception as e:
        logger.error(f"Server startup error: {str(e)}")
        sys.exit(1)

if __name__ == '__main__':
    main()