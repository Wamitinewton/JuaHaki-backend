import os
from pathlib import Path

class Config:
    """Base configuration class."""
    
    SECRET_KEY = os.getenv('SECRET_KEY', 'dev-secret-key-change-in-production')
    
    COHERE_API_KEY = os.getenv('COHERE_API_KEY')
    GEMINI_API_KEY = os.getenv('GEMINI_API_KEY')
    
    BASE_DIR = Path(__file__).parent
    DATA_DIR = BASE_DIR / 'data'
    EMBEDDINGS_DIR = BASE_DIR / 'embeddings'
    
    PDF_PATHS = {
        'constitution': str(DATA_DIR / 'Constitution.pdf'),
        'ten_years_assessment': str(DATA_DIR / 'Ten_Years_Assessment.pdf'),
        'human_rights_essays': str(DATA_DIR / 'Understanding_Human_Rights.pdf')
    }
    
    DEFAULT_TOP_K = 5
    MAX_QUESTION_LENGTH = 1000
    MAX_TOP_K = 20
    
    REQUEST_TIMEOUT = 30
    MAX_CONTENT_LENGTH = 16 * 1024 

class DevelopmentConfig(Config):
    """Development configuration."""
    DEBUG = True
    FLASK_ENV = 'development'

class ProductionConfig(Config):
    """Production configuration."""
    DEBUG = False
    FLASK_ENV = 'production'

class TestingConfig(Config):
    """Testing configuration."""
    TESTING = True
    DEBUG = True

config = {
    'development': DevelopmentConfig,
    'production': ProductionConfig,
    'testing': TestingConfig,
    'default': DevelopmentConfig
}