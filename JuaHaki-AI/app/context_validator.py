import re
from typing import Dict, List, Optional
from enum import Enum

class ContextRelevance(Enum):
    HIGHLY_RELEVANT = "highly_relevant"
    RELEVANT = "relevant"
    PARTIALLY_RELEVANT = "partially_relevant"
    NOT_RELEVANT = "not_relevant"

class ContextValidator:
    """Validates if user queries are relevant to civil rights and constitutional law with relaxed validation."""
    
    def __init__(self):
        self.civil_rights_keywords = {
            'constitutional', 'constitution', 'bill of rights', 'human rights',
            'civil rights', 'fundamental rights', 'freedoms', 'equality',
            'discrimination', 'justice', 'legal', 'law', 'court', 'judicial',
            'government', 'parliament', 'president', 'electoral', 'voting',
            'citizenship', 'devolution', 'county', 'national', 'public',
            'administration', 'corruption', 'transparency', 'accountability',
            'kenya', 'kenyan', 'article', 'chapter', 'section', 'act',
            'legislation', 'policy', 'governance', 'democracy', 'republic',
            'rights', 'civic', 'citizen', 'democratic', 'political', 'social',
            'economic', 'cultural', 'education', 'health', 'housing', 'water',
            'environment', 'land', 'property', 'assembly', 'association',
            'expression', 'speech', 'religion', 'belief', 'privacy', 'dignity',
            'life', 'security', 'liberty', 'fair', 'trial', 'arrest', 'detention',
            'police', 'authority', 'public', 'service', 'employment', 'trade',
            'union', 'strike', 'petition', 'demonstration', 'protest', 'media',
            'information', 'access', 'participation', 'representation'
        }
        
        self.legal_concepts = {
            'due process', 'fair trial', 'habeas corpus', 'natural justice',
            'separation of powers', 'checks and balances', 'rule of law',
            'constitutional review', 'judicial review', 'legislative process',
            'executive powers', 'parliamentary procedure', 'constitutional amendment',
            'public interest', 'common good', 'social justice', 'human dignity',
            'equal treatment', 'non-discrimination', 'affirmative action',
            'public participation', 'community involvement', 'civic engagement',
            'democratic governance', 'participatory democracy', 'representative democracy'
        }
        
        self.civic_terms = {
            'civic', 'civics', 'community', 'society', 'social', 'public',
            'democracy', 'democratic', 'participation', 'engagement', 'involvement',
            'representation', 'accountability', 'transparency', 'governance',
            'leadership', 'elected', 'officials', 'representatives', 'senators',
            'members', 'parliament', 'county', 'ward', 'constituency', 'devolution',
            'decentralization', 'local', 'municipal', 'council', 'assembly'
        }
        
        self.highly_irrelevant_patterns = [
            r'\b(explicit\s+sexual|pornographic|adult\s+content)\b',
            r'\b(gambling|betting|casino)\b',
            r'\b(drug\s+dealing|illegal\s+substances)\b',
            r'\b(violence|harm|weapons|murder)\b',
            r'\b(fraud|scam|illegal\s+money)\b'
        ]
        
        self.governance_patterns = [
            r'\b(how\s+does\s+government|government\s+work|political\s+system)\b',
            r'\b(citizen\s+rights|public\s+services|government\s+services)\b',
            r'\b(voting|elections|democracy|democratic)\b',
            r'\b(constitutional|constitution|bill\s+of\s+rights)\b',
            r'\b(public\s+policy|social\s+issues|community\s+problems)\b'
        ]
    
    def validate_context(self, query: str) -> Dict:
        """
        Legacy method - maintains backward compatibility but with relaxed validation.
        """
        return self.validate_context_relaxed(query)
    
    def validate_context_relaxed(self, query: str) -> Dict:
        """
        Relaxed validation that allows more questions through.
        Only rejects clearly inappropriate content.
        """
        query_lower = query.lower()
        
        for pattern in self.highly_irrelevant_patterns:
            if re.search(pattern, query_lower, re.IGNORECASE):
                return {
                    'relevance': ContextRelevance.NOT_RELEVANT,
                    'confidence': 0.9,
                    'reason': 'Query contains inappropriate content'
                }
        
        # Check for governance/civic patterns
        governance_matches = sum(1 for pattern in self.governance_patterns 
                               if re.search(pattern, query_lower, re.IGNORECASE))
        
        # Count keyword matches
        keyword_matches = sum(1 for keyword in self.civil_rights_keywords 
                            if keyword in query_lower)
        
        concept_matches = sum(1 for concept in self.legal_concepts 
                            if concept in query_lower)
        
        civic_matches = sum(1 for term in self.civic_terms 
                          if term in query_lower)
        
        total_matches = keyword_matches + (concept_matches * 2) + civic_matches + (governance_matches * 3)
        
        # More lenient scoring
        if total_matches >= 2 or governance_matches >= 1:
            relevance = ContextRelevance.HIGHLY_RELEVANT
            confidence = min(0.95, 0.6 + (total_matches * 0.05))
        elif total_matches >= 1:
            relevance = ContextRelevance.RELEVANT
            confidence = 0.5 + (total_matches * 0.1)
        elif any(word in query_lower for word in ['kenya', 'kenyan', 'government', 'law', 'public', 'social', 'community']):
            relevance = ContextRelevance.PARTIALLY_RELEVANT
            confidence = 0.4
        elif self._has_question_intent(query_lower):
            # If it's a genuine question, even if not clearly relevant, allow it
            relevance = ContextRelevance.PARTIALLY_RELEVANT
            confidence = 0.3
        else:
            relevance = ContextRelevance.NOT_RELEVANT
            confidence = 0.6  # Lower confidence for rejection
        
        return {
            'relevance': relevance,
            'confidence': confidence,
            'matches': total_matches,
            'reason': f'Found {total_matches} relevant matches in query'
        }
    
    def _has_question_intent(self, query: str) -> bool:
        """Check if the query has question intent (starts with question words, has question mark, etc.)"""
        question_indicators = [
            'what', 'how', 'why', 'when', 'where', 'who', 'which', 'can', 'could',
            'would', 'should', 'is', 'are', 'do', 'does', 'did', 'will', 'explain',
            'tell me', 'help me', 'i want to know', 'i need to understand'
        ]
        
        # Check for question mark
        if '?' in query:
            return True
        
        # Check for question words at the beginning
        first_words = query.strip().split()[:3]
        for word in first_words:
            if word.lower() in question_indicators:
                return True
        
        return False
    
    def is_completely_irrelevant(self, query: str) -> bool:
        """
        Quick check for completely irrelevant content.
        Returns True only for clearly inappropriate content.
        """
        query_lower = query.lower()
        
        for pattern in self.highly_irrelevant_patterns:
            if re.search(pattern, query_lower, re.IGNORECASE):
                return True
        
        # Check for some obviously unrelated topics
        unrelated_topics = [
            'recipe', 'cooking', 'food preparation', 'restaurant review',
            'movie review', 'celebrity gossip', 'sports scores', 'weather forecast',
            'stock prices', 'cryptocurrency', 'gaming', 'entertainment news'
        ]
        
        # Only reject if the query is clearly about these topics and has no civic/legal angle
        for topic in unrelated_topics:
            if topic in query_lower and not any(word in query_lower for word in ['law', 'legal', 'government', 'public', 'policy', 'regulation']):
                return True
        
        return False