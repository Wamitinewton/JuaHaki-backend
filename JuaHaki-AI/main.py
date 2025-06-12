import os
import sys
from pathlib import Path
from app.chatbot import CivilRightsChatbot

# Document paths
PDF_PATHS = {
    'constitution': "data/Constitution.pdf",
    'ten_years_assessment': "data/Ten_Years_Assessment.pdf",
    'human_rights_essays': "data/Understanding_Human_Rights.pdf"
}

EMBEDDINGS_DIR = "embeddings"

def check_requirements():
    """Check if required files exist."""
    missing_files = []
    
    for doc_type, pdf_path in PDF_PATHS.items():
        if not os.path.exists(pdf_path):
            missing_files.append(f"  - {pdf_path} ({doc_type})")
    
    if missing_files:
        print("Error: Missing PDF files:")
        for file in missing_files:
            print(file)
        print("\nPlease place all required PDFs in the data/ folder")
        return False
    
    if not os.path.exists('.env'):
        print("Error: .env file not found")
        print("Please create a .env file with your COHERE_API_KEY")
        return False
    
    return True

def main():
    print("🏛️  Kenyan Civil Rights AI Chatbot")
    print("=" * 50)
    print("Powered by multiple authoritative sources:")
    print("  📖 Kenyan Constitution 2010")
    print("  📊 Ten Years Implementation Assessment")
    print("  📝 Human Rights Essays & Case Studies")
    print("=" * 50)
    
    if not check_requirements():
        sys.exit(1)
    
    try:
        print("Initializing comprehensive civil rights chatbot...")
        chatbot = CivilRightsChatbot(EMBEDDINGS_DIR)
        
        if not chatbot.initialize(PDF_PATHS):
            print("Failed to initialize chatbot")
            sys.exit(1)
        
        chatbot.chat_loop()
        
    except KeyboardInterrupt:
        print("\nExiting...")
    except Exception as e:
        print(f"Error: {str(e)}")
        sys.exit(1)

if __name__ == "__main__":
    main()