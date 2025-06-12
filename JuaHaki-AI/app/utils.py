import fitz  # PyMuPDF
import re
from typing import List, Dict

def extract_pdf_text(pdf_path: str) -> str:
    """Extract text from PDF file."""
    doc = fitz.open(pdf_path)
    text = ""
    for page in doc:
        text += page.get_text()
    doc.close()
    return text

def chunk_text(text: str, chunk_size: int = 1000, overlap: int = 200) -> List[Dict]:
    """
    Split text into overlapping chunks for better retrieval.
    Returns list of dicts with text and metadata.
    """
    # Clean text
    text = re.sub(r'\s+', ' ', text)  
    text = text.strip()
    
    chunks = []
    start = 0
    chunk_id = 0
    
    while start < len(text):
        end = start + chunk_size
        
        if end < len(text):
            # Look for sentence endings within the last 100 characters
            sentence_end = text.rfind('.', start, end)
            if sentence_end != -1 and sentence_end > start + chunk_size - 100:
                end = sentence_end + 1
        
        chunk_text = text[start:end].strip()
        
        if chunk_text:
            chunks.append({
                'id': chunk_id,
                'text': chunk_text,
                'start_pos': start,
                'end_pos': end
            })
            chunk_id += 1
                # Move start position with overlap
        start = end - overlap
        if start >= len(text):
            break
    
    return chunks

def find_article_section(text: str) -> str:
    """
    Try to identify which article/section the text belongs to.
    This is a simple heuristic - can be improved based on your PDF structure.
    """
    patterns = [
        r'ARTICLE\s+(\d+[A-Z]*)',
        r'Article\s+(\d+[A-Z]*)',
        r'SECTION\s+(\d+)',
        r'Section\s+(\d+)',
        r'CHAPTER\s+(\d+)',
        r'Chapter\s+(\d+)'
    ]
    
    for pattern in patterns:
        match = re.search(pattern, text)
        if match:
            return match.group(0)
    
    return "Unknown Section"