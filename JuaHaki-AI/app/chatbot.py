import os
import cohere
from dotenv import load_dotenv
from .retriever import ConstitutionRetriever

load_dotenv()

class ConstitutionChatbot:
    def __init__(self, embeddings_dir: str = "embeddings"):
        api_key = os.getenv('COHERE_API_KEY')
        if not api_key:
            raise ValueError("COHERE_API_KEY not found in environment variables")
        
        self.cohere_client = cohere.Client("Tg24CjiX05lqsRDoLy7zlswF67diZVGWweEPAqED")
        self.retriever = ConstitutionRetriever(embeddings_dir)
        self.is_ready = False
    
    def initialize(self, pdf_path: str = None):
        """Initialize the chatbot with constitution data."""
        if self.retriever.initialize(pdf_path):
            self.is_ready = True
            print("Constitution chatbot is ready!")
            return True
        else:
            print("Failed to initialize chatbot")
            return False
    
    def generate_response(self, question: str, context: str) -> str:
        """Generate response using Cohere with retrieved context."""
        prompt = f"""You are a knowledgeable legal assistant specializing in the Kenyan Constitution. 
Your role is to provide accurate, detailed legal information based on the constitutional text provided.

CONTEXT FROM KENYAN CONSTITUTION:
{context}

QUESTION: {question}

INSTRUCTIONS:
1. Answer based primarily on the provided constitutional context
2. Cite specific articles, sections, or provisions when possible
3. If the context doesn't fully answer the question, say so clearly
4. Provide legal details and explanations where relevant
5. If asked about rights not explicitly mentioned, explain how they might be derived from broader constitutional principles
6. Be precise and professional in your legal analysis

ANSWER:"""

        try:
            response = self.cohere_client.generate(
                model='command-r-plus',
                prompt=prompt,
                max_tokens=1000,
                temperature=0.3,
                stop_sequences=["QUESTION:", "CONTEXT:"]
            )
            
            return response.generations[0].text.strip()
        
        except Exception as e:
            return f"Error generating response: {str(e)}"
    
    def ask(self, question: str, top_k: int = 3) -> str:
        """Ask a question about the Kenyan Constitution."""
        if not self.is_ready:
            return "Chatbot not initialized. Please run initialize() first."
        
        # Retrieve relevant context
        print("Searching for relevant constitutional sections...")
        context = self.retriever.get_relevant_context(question, top_k=top_k)
        
        # Generate response
        print("Generating response...")
        response = self.generate_response(question, context)
        
        return response
    
    def chat_loop(self):
        """Start an interactive chat session."""
        if not self.is_ready:
            print("Chatbot not initialized!")
            return
        
        print("=== Kenyan Constitution Legal Assistant ===")
        print("Ask questions about the Kenyan Constitution. Type 'quit' to exit.\n")
        
        while True:
            try:
                question = input("\nYour question: ").strip()
                
                if question.lower() in ['quit', 'exit', 'q']:
                    print("Goodbye!")
                    break
                
                if not question:
                    print("Please enter a question.")
                    continue
                
                print("\n" + "="*60)
                response = self.ask(question)
                print("ANSWER:")
                print(response)
                print("="*60)
                
            except KeyboardInterrupt:
                print("\nGoodbye!")
                break
            except Exception as e:
                print(f"Error: {str(e)}")
                continue