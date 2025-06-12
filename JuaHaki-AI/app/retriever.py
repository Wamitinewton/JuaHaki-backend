from typing import List, Dict, Optional
from .embedder import MultiDocumentEmbedder
import logging

logger = logging.getLogger(__name__)

class CivilRightsRetriever:
    def __init__(self, embeddings_dir: str = "embeddings"):
        self.embedder = MultiDocumentEmbedder(embeddings_dir)
        self.is_ready = False
        self.document_types = {
            'constitution': 'Kenyan Constitution 2010',
            'ten_years_assessment': 'Ten Years Assessment Report',
            'human_rights_essays': 'Understanding Human Rights Essays'
        }
    
    def initialize(self, pdf_paths: Dict[str, str] = None) -> bool:
        """Initialize the retriever by loading or creating embeddings."""
        try:
            if self.embedder.load_index():
                self.is_ready = True
                logger.info("Loaded existing document embeddings")
                return True
            
            if pdf_paths:
                logger.info("Creating new document embeddings...")
                from .utils import extract_pdf_text
                
                documents = {}
                for doc_type, pdf_path in pdf_paths.items():
                    documents[doc_type] = extract_pdf_text(pdf_path)
                
                self.embedder.create_embeddings(documents)
                self.is_ready = True
                logger.info("Successfully created new embeddings")
                return True
            
            logger.error("No embeddings found and no PDF paths provided")
            return False
            
        except Exception as e:
            logger.error(f"Retriever initialization error: {str(e)}")
            return False
    
    def retrieve_context(self, query: str, top_k: int = 5, doc_type_filter: Optional[str] = None) -> List[Dict]:
        """Retrieve relevant context for a given query."""
        if not self.is_ready:
            raise ValueError("Retriever not initialized. Call initialize() first.")
        
        return self.embedder.search(query, top_k=top_k, doc_type_filter=doc_type_filter)
    
    def format_context(self, results: List[Dict]) -> str:
        """Format retrieved results into a context string."""
        if not results:
            return "No relevant context found."
        
        context_parts = []
        for i, result in enumerate(results, 1):
            doc_name = self.document_types.get(result['doc_type'], result['doc_type'])
            section = result.get('section', 'Unknown Section')
            text = result['text']
            score = result.get('relevance_score', 0)
            
            context_parts.append(
                f"--- Context {i} ---\n"
                f"Source: {doc_name}\n"
                f"Section: {section}\n"
                f"Relevance: {score:.3f}\n"
                f"Content: {text}\n"
            )
        
        return "\n".join(context_parts)
    
    def get_relevant_context(self, query: str, top_k: int = 5, doc_type_filter: Optional[str] = None) -> str:
        """Get formatted relevant context for a query."""
        results = self.retrieve_context(query, top_k, doc_type_filter)
        return self.format_context(results)
    
    def get_document_stats(self) -> Dict:
        """Get statistics about loaded documents."""
        if not self.is_ready:
            return {}
        
        stats = {}
        for metadata in self.embedder.metadata:
            doc_type = metadata['doc_type']
            if doc_type not in stats:
                stats[doc_type] = 0
            stats[doc_type] += 1
        
        return stats