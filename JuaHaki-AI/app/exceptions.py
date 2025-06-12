class ChatbotError(Exception):
    """Base exception for chatbot-related errors."""
    pass

class ValidationError(Exception):
    """Exception raised for request validation errors."""
    pass

class InitializationError(ChatbotError):
    """Exception raised when chatbot initialization fails."""
    pass

class DocumentError(ChatbotError):
    """Exception raised for document processing errors."""
    pass

class APIError(ChatbotError):
    """Exception raised for external API errors."""
    pass