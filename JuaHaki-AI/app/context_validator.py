import re
from typing import Dict, List, Optional
from enum import Enum

class ContextRelevance(Enum):
    HIGHLY_RELEVANT = "highly_relevant"
    RELEVANT = "relevant"
    PARTIALLY_RELEVANT = "partially_relevant"
    NOT_RELEVANT = "not_relevant"

class ContextValidator:
    """Validates if user queries are relevant to civil rights and constitutional law."""
    
    def __init__(self):
        self.civil_rights_keywords = {
            'constitutional', 'constitution', 'bill of rights', 'human rights',
            'civil rights', 'fundamental rights', 'freedoms', 'equality',
            'discrimination', 'justice', 'legal', 'law', 'court', 'judicial',
            'government', 'parliament', 'president', 'electoral', 'voting',
            'citizenship', 'devolution', 'county', 'national', 'public',
            'administration', 'corruption', 'transparency', 'accountability',
            'kenya', 'kenyan', 'article', 'chapter', 'section', 'act',
            'legislation', 'policy', 'governance', 'democracy', 'republic'
        }
        
        self.legal_concepts = {
            'due process', 'fair trial', 'habeas corpus', 'natural justice',
            'separation of powers', 'checks and balances', 'rule of law',
            'constitutional review', 'judicial review', 'legislative process',
            'executive powers', 'parliamentary procedure', 'constitutional amendment'
        }
        
        self.irrelevant_patterns = [
            r'\b(sex|sexual|intercourse|porn|adult|explicit)\b',
            r'\b(dating|relationship|romance|love)\b',
            r'\b(cooking|recipe|food|restaurant)\b',
            r'\b(sports|football|soccer|basketball)\b',
            r'\b(entertainment|movie|music|celebrity)\b',
            r'\b(technology|software|programming|computer)\b',
            r'\b(business|marketing|sales|profit)\b',
            r'\b(health|medical|disease|treatment)\b'
        ]
    
    def validate_context(self, query: str) -> Dict:
        """
        Validate if the query is relevant to civil rights and constitutional law.
        """
        query_lower = query.lower()
        
        for pattern in self.irrelevant_patterns:
            if re.search(pattern, query_lower, re.IGNORECASE):
                return {
                    'relevance': ContextRelevance.NOT_RELEVANT,
                    'confidence': 0.9,
                    'reason': 'Query contains content outside the scope of civil rights and constitutional law'
                }
        
        keyword_matches = sum(1 for keyword in self.civil_rights_keywords 
                            if keyword in query_lower)
        
        concept_matches = sum(1 for concept in self.legal_concepts 
                            if concept in query_lower)
        
        total_matches = keyword_matches + (concept_matches * 2)
        
        if total_matches >= 3:
            relevance = ContextRelevance.HIGHLY_RELEVANT
            confidence = min(0.95, 0.7 + (total_matches * 0.05))
        elif total_matches >= 1:
            relevance = ContextRelevance.RELEVANT
            confidence = 0.6 + (total_matches * 0.1)
        elif any(word in query_lower for word in ['kenya', 'kenyan', 'government', 'law']):
            relevance = ContextRelevance.PARTIALLY_RELEVANT
            confidence = 0.4
        else:
            relevance = ContextRelevance.NOT_RELEVANT
            confidence = 0.8
        
        return {
            'relevance': relevance,
            'confidence': confidence,
            'matches': total_matches,
            'reason': f'Found {total_matches} relevant matches in query'
        }