import os
import pickle
import numpy as np
import faiss
from sentence_transformers import SentenceTransformer
from typing import List, Dict
from .utils import chunk_text, find_article_section

class ConstitutionEmbedder:
    def __init__(self, embeddings_dir: str = "embeddings"):
        self.embeddings_dir = embeddings_dir
        self.index_path = os.path.join(embeddings_dir, "constitution_index.faiss")
        self.metadata_path = os.path.join(embeddings_dir, "metadata.pkl")
        
        self.encoder = SentenceTransformer('all-MiniLM-L6-v2')
        self.embedding_dim = 384
        
        self.index = None
        self.metadata = []
        
        os.makedirs(embeddings_dir, exist_ok=True)
    
    def create_embeddings(self, text: str, chunk_size: int = 800, overlap: int = 150):
        """Create embeddings from constitution text."""
        print("Chunking text...")
        chunks = chunk_text(text, chunk_size=chunk_size, overlap=overlap)
        print(f"Created {len(chunks)} chunks")
        
        print("Generating embeddings...")
        texts = [chunk['text'] for chunk in chunks]
        embeddings = self.encoder.encode(texts, show_progress_bar=True)
        
        print("Creating FAISS index...")
        self.index = faiss.IndexFlatIP(self.embedding_dim)  # Inner Product (cosine similarity)
        
        faiss.normalize_L2(embeddings)
        self.index.add(embeddings.astype('float32'))
        
        self.metadata = []
        for i, chunk in enumerate(chunks):
            section = find_article_section(chunk['text'])
            self.metadata.append({
                'chunk_id': chunk['id'],
                'text': chunk['text'],
                'section': section,
                'start_pos': chunk['start_pos'],
                'end_pos': chunk['end_pos']
            })
        
        self.save_index()
        print(f"Embeddings created and saved! Total chunks: {len(chunks)}")
    
    def load_index(self) -> bool:
        """Load existing FAISS index and metadata."""
        try:
            if os.path.exists(self.index_path) and os.path.exists(self.metadata_path):
                self.index = faiss.read_index(self.index_path)
                with open(self.metadata_path, 'rb') as f:
                    self.metadata = pickle.load(f)
                print(f"Loaded existing index with {len(self.metadata)} chunks")
                return True
        except Exception as e:
            print(f"Error loading index: {e}")
        return False
    
    def save_index(self):
        """Save FAISS index and metadata."""
        faiss.write_index(self.index, self.index_path)
        with open(self.metadata_path, 'wb') as f:
            pickle.dump(self.metadata, f)
    
    def search(self, query: str, top_k: int = 3) -> List[Dict]:
        """Search for relevant chunks given a query."""
        if self.index is None:
            raise ValueError("Index not loaded. Call load_index() or create_embeddings() first.")
        
        query_embedding = self.encoder.encode([query])
        faiss.normalize_L2(query_embedding)
        
        scores, indices = self.index.search(query_embedding.astype('float32'), top_k)
        
        results = []
        for score, idx in zip(scores[0], indices[0]):
            if idx < len(self.metadata):  # Valid index
                result = self.metadata[idx].copy()
                result['relevance_score'] = float(score)
                results.append(result)
        
        return results