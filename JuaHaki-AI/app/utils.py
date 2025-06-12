import fitz  # PyMuPDF
import re
from typing import List, Dict, Optional
from pathlib import Path

def extract_pdf_text(pdf_path: str) -> str:
    """Extract text from PDF file."""
    doc = fitz.open(pdf_path)
    text = ""
    for page in doc:
        text += page.get_text()
    doc.close()
    return text

def chunk_text(text: str, chunk_size: int = 1000, overlap: int = 200, doc_type: str = "constitution") -> List[Dict]:
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
                'end_pos': end,
                'doc_type': doc_type
            })
            chunk_id += 1
        
        # Move start position with overlap
        start = end - overlap
        if start >= len(text):
            break
    
    return chunks

def find_document_section(text: str, doc_type: str) -> str:
    """
    Identify which section the text belongs to based on document type.
    """
    if doc_type == "constitution":
        patterns = [
            r'ARTICLE\s+(\d+[A-Z]*)',
            r'Article\s+(\d+[A-Z]*)',
            r'SECTION\s+(\d+)',
            r'Section\s+(\d+)',
            r'CHAPTER\s+(\d+)',
            r'Chapter\s+(\d+)'
        ]
    elif doc_type == "ten_years_assessment":
        patterns = [
            r'Chapter\s+(\d+)',
            r'CHAPTER\s+(\d+)',
            r'Section\s+(\d+\.\d+)',
            r'(\d+\.\d+\s+[A-Z][a-z\s]+)',
        ]
    elif doc_type == "human_rights_essays":
        patterns = [
            r'Essay\s+(\d+)',
            r'Story\s+(\d+)',
            r'Case\s+Study\s+(\d+)',
            r'Chapter\s+(\d+)'
        ]
    else:
        patterns = [r'Section\s+(\d+)', r'Chapter\s+(\d+)']
    
    for pattern in patterns:
        match = re.search(pattern, text)
        if match:
            return match.group(0)
    
    return f"Unknown Section ({doc_type})"