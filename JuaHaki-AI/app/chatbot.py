import os
import asyncio
import google.generativeai as genai
import cohere
from dotenv import load_dotenv
from typing import Optional, Dict, List, Tuple
import logging
from .retriever import CivilRightsRetriever
from .context_validator import ContextValidator, ContextRelevance
from .exceptions import ChatbotError, InitializationError, APIError

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

load_dotenv()

class CivilRightsChatbot:
    def __init__(self, embeddings_dir: str = "embeddings"):
        """Initialize the Civil Rights Chatbot."""
        try:
            cohere_api_key = os.getenv('COHERE_API_KEY')
            if not cohere_api_key:
                raise InitializationError("COHERE_API_KEY not found in environment variables")
            
            gemini_api_key = os.getenv('GEMINI_API_KEY')
            if not gemini_api_key:
                raise InitializationError("GEMINI_API_KEY not found in environment variables")
            
            self.cohere_client = cohere.Client(cohere_api_key)
            genai.configure(api_key=gemini_api_key)
            self.gemini_model = genai.GenerativeModel('gemini-1.5-flash')
            
            self.retriever = CivilRightsRetriever(embeddings_dir)
            self.context_validator = ContextValidator()
            self.is_ready = False
            self.conversation_history = []
            
            logger.info("Civil Rights Chatbot initialized")
            
        except Exception as e:
            logger.error(f"Chatbot initialization error: {str(e)}")
            raise InitializationError(f"Failed to initialize chatbot: {str(e)}")
    
    def initialize(self, pdf_paths: Dict[str, str] = None) -> bool:
        """Initialize the chatbot with document sources."""
        try:
            if self.retriever.initialize(pdf_paths):
                self.is_ready = True
                stats = self.retriever.get_document_stats()
                total_chunks = sum(stats.values())
                logger.info(f"Chatbot ready with {total_chunks} document chunks")
                return True
            else:
                logger.error("Failed to initialize document retriever")
                return False
        except Exception as e:
            logger.error(f"Initialization error: {str(e)}")
            raise InitializationError(f"Failed to initialize documents: {str(e)}")
    
    def _analyze_user_query(self, question: str) -> Dict:
        """Analyze user query for better retrieval and response generation."""
        analysis_prompt = f"""
        Analyze this user question about Kenyan civil rights and constitutional law:

        QUESTION: "{question}"

        Provide analysis in this format:
        INTENT: [main legal intent]
        KEY_CONCEPTS: [concept1, concept2, concept3]
        ENHANCED_QUERY: [optimized search query]
        DOCUMENT_FOCUS: [constitution/ten_years_assessment/human_rights_essays/all]
        COMPLEXITY: [1-5]
        CONTEXT_NEEDED: [brief description]
        """
        
        try:
            response = self.gemini_model.generate_content(analysis_prompt)
            return self._parse_query_analysis(response.text)
        except Exception as e:
            logger.warning(f"Query analysis error: {str(e)}")
            return {
                'intent': 'general_inquiry',
                'key_concepts': [question],
                'enhanced_query': question,
                'document_focus': 'all',
                'complexity': 3,
                'context_needed': 'Standard legal context'
            }
    
    def _parse_query_analysis(self, analysis_text: str) -> Dict:
        """Parse Gemini's analysis response."""
        lines = analysis_text.strip().split('\n')
        analysis = {}
        
        for line in lines:
            if ':' in line:
                key, value = line.split(':', 1)
                key = key.strip().lower().replace(' ', '_')
                value = value.strip()
                
                if key == 'key_concepts':
                    value = [concept.strip() for concept in value.replace('[', '').replace(']', '').split(',')]
                elif key == 'complexity':
                    try:
                        value = int(value)
                    except:
                        value = 3
                
                analysis[key] = value
        
        defaults = {
            'intent': 'general_inquiry',
            'key_concepts': ['civil rights'],
            'enhanced_query': '',
            'document_focus': 'all',
            'complexity': 3,
            'context_needed': 'Standard legal context'
        }
        
        for key, default_value in defaults.items():
            if key not in analysis:
                analysis[key] = default_value
        
        return analysis
    
    def _generate_comprehensive_response(self, question: str, document_context: str, analysis: Dict) -> str:
        """Generate comprehensive response using document context."""
        
        complexity_guidance = {
            1: "Provide a straightforward, basic explanation in simple terms",
            2: "Include some legal context and examples in accessible language", 
            3: "Provide comprehensive analysis with clear explanations and citations",
            4: "Include nuanced legal interpretation with detailed context",
            5: "Provide expert-level analysis with comprehensive legal reasoning"
        }
        
        prompt = f"""You are an expert legal assistant specializing in Kenyan civil rights and constitutional law.

USER QUESTION: {question}

QUERY ANALYSIS:
- Intent: {analysis.get('intent', 'general_inquiry')}
- Key Concepts: {', '.join(analysis.get('key_concepts', []))}
- Complexity Level: {analysis.get('complexity', 3)}/5

GUIDANCE: {complexity_guidance.get(analysis.get('complexity', 3), 'Provide comprehensive analysis')}

DOCUMENT CONTEXT:
{document_context}

INSTRUCTIONS:
1. Provide a comprehensive, flowing response that directly addresses the user's question
2. Write in clear, natural prose without section headers or structural formatting
3. Integrate citations to specific constitutional articles, sections, or legal provisions naturally within the text
4. Explain both legal theory and practical implications in a coherent narrative
5. Use accessible language while maintaining legal accuracy
6. Include relevant examples when possible, drawn from the document context
7. Base your analysis strictly on the provided document context
8. Write as a continuous, well-structured response without artificial divisions or headings

Provide a thorough, well-sourced response in natural flowing text based solely on the document context:"""

        try:
            response = self.cohere_client.generate(
                model='command-r-plus',
                prompt=prompt,
                max_tokens=1500,
                temperature=0.3,
                stop_sequences=["USER QUESTION:", "DOCUMENT CONTEXT:"]
            )
            
            return response.generations[0].text.strip()
        
        except Exception as e:
            logger.error(f"Response generation error: {str(e)}")
            raise APIError(f"Failed to generate response: {str(e)}")
    
    def ask(self, question: str, top_k: int = 5, doc_filter: Optional[str] = None) -> str:
        """
        Ask a question using the document-based system.
        
        Args:
            question: The question to ask
            top_k: Number of top documents to retrieve
            doc_filter: Optional filter for document type
            
        Returns:
            String response to the question
            
        Raises:
            ChatbotError: If chatbot is not ready or processing fails
        """
        if not self.is_ready:
            raise ChatbotError("Chatbot not initialized. Please initialize first.")
        
        try:
            validation = self.context_validator.validate_context(question)
            
            if validation['relevance'] == ContextRelevance.NOT_RELEVANT:
                return (
                    "I'm sorry, but your question appears to be outside the scope of civil rights "
                    "and constitutional law. I'm designed to help with questions about Kenyan "
                    "constitutional matters, human rights, governance, and related legal topics. "
                    "Please ask a question related to these areas."
                )
            
            analysis = self._analyze_user_query(question)
            
            search_query = analysis.get('enhanced_query', question)
            if not search_query.strip():
                search_query = question
            
            doc_type_filter = doc_filter
            if not doc_type_filter:
                focus = analysis.get('document_focus', 'all')
                if focus != 'all':
                    doc_type_filter = focus
            
            document_context = self.retriever.get_relevant_context(
                search_query, 
                top_k=top_k, 
                doc_type_filter=doc_type_filter
            )
            
            final_response = self._generate_comprehensive_response(
                question, document_context, analysis
            )
            
            self.conversation_history.append({
                'question': question,
                'analysis': analysis,
                'response': final_response,
                'validation': validation
            })
            
            if len(self.conversation_history) > 5:
                self.conversation_history.pop(0)
            
            return final_response
            
        except APIError as e:
            logger.error(f"API error processing question: {str(e)}")
            raise e
        except Exception as e:
            logger.error(f"Error processing question: {str(e)}")
            raise ChatbotError(f"Failed to process question: {str(e)}")
    
    def ask_constitutional_only(self, question: str, top_k: int = 3) -> str:
        """Ask a question using only the constitution."""
        return self.ask(question, top_k=top_k, doc_filter='constitution')
    
    def get_conversation_summary(self) -> str:
        """Get a summary of recent conversation."""
        if not self.conversation_history:
            return "No conversation history available."
        
        history_text = "\n\n".join([
            f"Q: {exchange['question']}\nA: {exchange['response'][:200]}..."
            for exchange in self.conversation_history[-3:]
        ])
        
        summary_prompt = f"""
        Summarize this conversation about Kenyan civil rights and constitutional law:
        
        {history_text}
        
        Provide a brief summary highlighting:
        1. Main topics discussed
        2. Key legal points covered
        3. Any recurring themes
        
        Keep it concise (2-3 sentences).
        """
        
        try:
            response = self.gemini_model.generate_content(summary_prompt)
            return response.text.strip()
        except Exception as e:
            logger.error(f"Summary generation error: {str(e)}")
            raise APIError(f"Failed to generate summary: {str(e)}")
    
    def get_health_status(self) -> Dict:
        """Get health status of the chatbot."""
        return {
            'is_ready': self.is_ready,
            'conversation_history_count': len(self.conversation_history),
            'document_stats': self.retriever.get_document_stats() if self.is_ready else {}
        }