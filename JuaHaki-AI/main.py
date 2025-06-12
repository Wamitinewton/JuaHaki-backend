import os
import sys
import logging
from pathlib import Path
from app.chatbot import CivilRightsChatbot

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

PDF_PATHS = {
    'constitution': "data/Constitution.pdf",
    'ten_years_assessment': "data/Ten_Years_Assessment.pdf",
    'human_rights_essays': "data/Understanding_Human_Rights.pdf"
}

EMBEDDINGS_DIR = "embeddings"

def check_requirements() -> bool:
    """Check if required files and environment variables exist."""
    missing_files = []
    
    for doc_type, pdf_path in PDF_PATHS.items():
        if not os.path.exists(pdf_path):
            missing_files.append(f"  - {pdf_path} ({doc_type})")
    
    if missing_files:
        logger.error("Missing PDF files:")
        for file in missing_files:
            print(file)
        print("\nPlease place all required PDFs in the data/ folder")
        return False
    
    if not os.path.exists('.env'):
        logger.error(".env file not found")
        print("Please create a .env file with your COHERE_API_KEY and GEMINI_API_KEY")
        return False
    
    from dotenv import load_dotenv
    load_dotenv()
    
    if not os.getenv('COHERE_API_KEY'):
        logger.error("COHERE_API_KEY not found in .env file")
        return False
    
    if not os.getenv('GEMINI_API_KEY'):
        logger.error("GEMINI_API_KEY not found in .env file")
        return False
    
    return True

def main():
    """Main application entry point."""
    print("🏛️  Kenyan Civil Rights AI Chatbot")
    print("=" * 50)
    print("Enhanced with multiple authoritative sources:")
    print("  📖 Kenyan Constitution 2010")
    print("  📊 Ten Years Implementation Assessment")
    print("  📝 Human Rights Essays & Case Studies")
    print("  🌐 Web Search Integration")
    print("=" * 50)
    
    if not check_requirements():
        sys.exit(1)
    
    try:
        logger.info("Initializing Civil Rights Chatbot...")
        chatbot = CivilRightsChatbot(EMBEDDINGS_DIR)
        
        if not chatbot.initialize(PDF_PATHS):
            logger.error("Failed to initialize chatbot")
            sys.exit(1)
        
        chatbot.chat_loop()
        
    except KeyboardInterrupt:
        print("\nExiting...")
    except Exception as e:
        logger.error(f"Application error: {str(e)}")
        sys.exit(1)

if __name__ == "__main__":
    main()