import os
import cohere
from dotenv import load_dotenv
from typing import Optional, Dict
from .retriever import CivilRightsRetriever

load_dotenv()

class CivilRightsChatbot:
    def __init__(self, embeddings_dir: str = "embeddings"):
        api_key = os.getenv('COHERE_API_KEY')
        if not api_key:
            raise ValueError("COHERE_API_KEY not found in environment variables")
        
        self.cohere_client = cohere.Client(api_key)
        self.retriever = CivilRightsRetriever(embeddings_dir)
        self.is_ready = False
    
    def initialize(self, pdf_paths: Dict[str, str] = None):
        """Initialize the chatbot with multiple document sources."""
        if self.retriever.initialize(pdf_paths):
            self.is_ready = True
            print("Civil Rights AI Chatbot is ready!")
            print("\nDocument Statistics:")
            stats = self.retriever.get_document_stats()
            for doc_type, count in stats.items():
                doc_name = self.retriever.document_types.get(doc_type, doc_type)
                print(f"  - {doc_name}: {count} chunks")
            return True
        else:
            print("Failed to initialize chatbot")
            return False
    
    def generate_response(self, question: str, context: str) -> str:
        """Generate response using Cohere with retrieved context from multiple sources."""
        prompt = f"""You are an expert legal assistant specializing in Kenyan civil rights and constitutional law. 
You have access to multiple authoritative sources including the Kenyan Constitution 2010, implementation assessments, and human rights case studies.

Your expertise covers:
- Constitutional rights and freedoms
- Implementation challenges and successes
- Real-world human rights experiences
- Legal precedents and interpretations
- Civil rights advocacy and protection

RELEVANT CONTEXT FROM MULTIPLE SOURCES:
{context}

QUESTION: {question}

INSTRUCTIONS:
1. Provide comprehensive answers drawing from all relevant sources
2. Cite specific articles, sections, or cases when available
3. Reference both constitutional provisions AND practical implementation insights
4. Include real-world examples or case studies when relevant
5. Explain how constitutional principles apply in practice
6. If information is incomplete, clearly state what additional context might be needed
7. Maintain professional legal accuracy while being accessible
8. Consider both legal theory and practical civil rights challenges

Provide a thorough, well-sourced response that demonstrates the depth of Kenya's civil rights framework:"""

        try:
            response = self.cohere_client.generate(
                model='command-r-plus',
                prompt=prompt,
                max_tokens=1200,
                temperature=0.3,
                stop_sequences=["QUESTION:", "CONTEXT:"]
            )
            
            return response.generations[0].text.strip()
        
        except Exception as e:
            return f"Error generating response: {str(e)}"
    
    def ask(self, question: str, top_k: int = 5, doc_filter: Optional[str] = None) -> str:
        """
        Ask a question about Kenyan civil rights.
        doc_filter: Optional filter ('constitution', 'ten_years_assessment', 'human_rights_essays')
        """
        if not self.is_ready:
            return "Chatbot not initialized. Please run initialize() first."
        
        # Retrieve relevant context from multiple sources
        print("Searching across multiple documents...")
        context = self.retriever.get_relevant_context(question, top_k=top_k, doc_type_filter=doc_filter)
        
        # Generate comprehensive response
        print("Generating comprehensive response...")
        response = self.generate_response(question, context)
        
        return response
    
    def ask_constitutional_only(self, question: str, top_k: int = 3) -> str:
        """Ask a question using only the constitution."""
        return self.ask(question, top_k=top_k, doc_filter='constitution')
    
    def ask_with_implementation_context(self, question: str, top_k: int = 5) -> str:
        """Ask a question including implementation assessment insights."""
        return self.ask(question, top_k=top_k, doc_filter=None)
    
    def chat_loop(self):
        """Start an interactive chat session."""
        if not self.is_ready:
            print("Chatbot not initialized!")
            return
        
        print("=== Kenyan Civil Rights AI Assistant ===")
        print("Ask questions about civil rights, constitutional law, and human rights in Kenya.")
        print("Commands:")
        print("  - 'quit' or 'exit': End session")
        print("  - 'stats': Show document statistics")
        print("  - 'const [question]': Search only constitution")
        print("  - 'help': Show this help\n")
        
        while True:
            try:
                user_input = input("\nYour question: ").strip()
                
                if user_input.lower() in ['quit', 'exit', 'q']:
                    print("Goodbye!")
                    break
                
                if user_input.lower() == 'help':
                    print("\nCommands:")
                    print("  - General questions: Just type your question")
                    print("  - Constitution only: 'const [your question]'")
                    print("  - Document stats: 'stats'")
                    print("  - Exit: 'quit' or 'exit'")
                    continue
                
                if user_input.lower() == 'stats':
                    stats = self.retriever.get_document_stats()
                    print("\nDocument Statistics:")
                    for doc_type, count in stats.items():
                        doc_name = self.retriever.document_types.get(doc_type, doc_type)
                        print(f"  - {doc_name}: {count} chunks")
                    continue
                
                if not user_input:
                    print("Please enter a question.")
                    continue
                
                # Handle constitution-only queries
                if user_input.lower().startswith('const '):
                    question = user_input[6:].strip()
                    if question:
                        print("\n" + "="*80)
                        print("SEARCHING CONSTITUTION ONLY...")
                        response = self.ask_constitutional_only(question)
                        print("ANSWER:")
                        print(response)
                        print("="*80)
                    else:
                        print("Please provide a question after 'const'")
                    continue
                
                # Regular comprehensive search
                print("\n" + "="*80)
                print("SEARCHING ALL SOURCES...")
                response = self.ask(user_input)
                print("COMPREHENSIVE ANSWER:")
                print(response)
                print("="*80)
                
            except KeyboardInterrupt:
                print("\nGoodbye!")
                break
            except Exception as e:
                print(f"Error: {str(e)}")
                continue