from typing import List, Dict
from .embedder import ConstitutionEmbedder

class ConstitutionRetriever:
    def __init__(self, embeddings_dir: str = "embeddings"):
        self.embedder = ConstitutionEmbedder(embeddings_dir)
        self.is_ready = False
    
    def initialize(self, pdf_path: str = None):
        """Initialize the retriever by loading or creating embeddings."""
        if self.embedder.load_index():
            self.is_ready = True
            return True
        
        if pdf_path:
            print("No existing embeddings found. Creating new ones...")
            from .utils import extract_pdf_text
            
            text = extract_pdf_text(pdf_path)
            self.embedder.create_embeddings(text)
            self.is_ready = True
            return True
        
        print("No embeddings found and no PDF path provided.")
        return False
    
    def retrieve_context(self, query: str, top_k: int = 3) -> List[Dict]:
        """Retrieve relevant context for a given query."""
        if not self.is_ready:
            raise ValueError("Retriever not initialized. Call initialize() first.")
        
        results = self.embedder.search(query, top_k=top_k)
        return results
    
    def format_context(self, results: List[Dict]) -> str:
        """Format retrieved results into a context string."""
        if not results:
            return "No relevant context found."
        
        context_parts = []
        for i, result in enumerate(results, 1):
            section = result.get('section', 'Unknown Section')
            text = result['text']
            score = result.get('relevance_score', 0)
            
            context_parts.append(
                f"--- Context {i} (Section: {section}, Relevance: {score:.3f}) ---\n{text}\n"
            )
        
        return "\n".join(context_parts)
    
    def get_relevant_context(self, query: str, top_k: int = 3) -> str:
        """Get formatted relevant context for a query."""
        results = self.retrieve_context(query, top_k)
        return self.format_context(results)