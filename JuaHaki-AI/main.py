import os
import sys
from app.chatbot import ConstitutionChatbot

PDF_PATH = "data/Constitution.pdf"
EMBEDDINGS_DIR = "embeddings"

def check_requirements():
    """Check if required files exist."""
    if not os.path.exists(PDF_PATH):
        print(f"Error: PDF file not found at '{PDF_PATH}'")
        print("Please place your Kenyan Constitution PDF in the data/ folder")
        return False
    
    if not os.path.exists('.env'):
        print("Error: .env file not found")
        print("Please create a .env file with your COHERE_API_KEY")
        return False
    
    return True

def main():
    print("🏛️  Kenyan Constitution RAG Chatbot (Cohere)")
    print("=" * 50)
    
    if not check_requirements():
        sys.exit(1)
    
    try:
        print("Initializing chatbot...")
        chatbot = ConstitutionChatbot(EMBEDDINGS_DIR)
        
        if not chatbot.initialize(PDF_PATH):
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