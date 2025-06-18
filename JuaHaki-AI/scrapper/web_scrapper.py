import requests
import ssl
import urllib3
from bs4 import BeautifulSoup
from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
import time
from typing import List, Dict, Optional
from langchain_community.document_loaders import UnstructuredURLLoader
from langchain.schema import Document
from langchain.text_splitter import RecursiveCharacterTextSplitter
from langchain_community.embeddings import SentenceTransformerEmbeddings
from langchain_community.vectorstores import FAISS
from sentence_transformers import SentenceTransformer
import logging
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry

# Disable SSL warnings
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

class WebContentLoader:
    """
    A comprehensive web content loader with multiple methods to handle different types of websites.
    Returns data in LangChain Document format for easy integration with text processing pipelines.
    """
    
    def __init__(self, urls: List[str], headers: Optional[Dict] = None):
        """
        Initialize the WebContentLoader.
        
        Args:
            urls: List of URLs to scrape
            headers: Optional custom headers dictionary
        """
        self.urls = urls
        self.headers = headers or {
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36',
            'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8',
            'Accept-Language': 'en-US,en;q=0.5',
            'Accept-Encoding': 'gzip, deflate',
            'Connection': 'keep-alive',
            'Upgrade-Insecure-Requests': '1',
        }
        self.documents = []
        self.logger = self._setup_logger()
    
    def _setup_logger(self):
        """Setup logging for the class."""
        logger = logging.getLogger(__name__)
        logger.setLevel(logging.INFO)
        if not logger.handlers:
            handler = logging.StreamHandler()
            formatter = logging.Formatter('%(asctime)s - %(levelname)s - %(message)s')
            handler.setFormatter(formatter)
            logger.addHandler(handler)
        return logger
    
    def _create_document(self, url: str, content: str, method: str) -> Document:
        """
        Create a LangChain Document from scraped content.
        
        Args:
            url: Source URL
            content: Scraped content
            method: Method used for scraping
            
        Returns:
            LangChain Document object
        """
        return Document(
            page_content=content,
            metadata={
                'source': url,
                'method': method,
                'length': len(content),
                'timestamp': time.time()
            }
        )
    
    def load_with_custom_requests(self) -> List[Document]:
        """
        Load content using custom requests with retry logic and SSL handling.
        
        Returns:
            List of LangChain Document objects
        """
        
        
        self.logger.info("🔄 Loading content with custom requests method")
        
        # Create session with retry strategy
        session = requests.Session()
        retry_strategy = Retry(
            total=3,
            backoff_factor=1,
            status_forcelist=[429, 500, 502, 503, 504],
        )
        adapter = HTTPAdapter(max_retries=retry_strategy)
        session.mount("http://", adapter)
        session.mount("https://", adapter)
        
        documents = []
        
        for url in self.urls:
            try:
                self.logger.info(f"Fetching: {url}")
                
                response = session.get(
                    url,
                    headers=self.headers,
                    verify=False,  # Disable SSL verification
                    timeout=30
                )
                response.raise_for_status()
                
                # Check if content appears to be blocked
                content = response.text
                if self._is_content_blocked(content):
                    self.logger.warning(f"Content appears blocked for: {url}")
                    continue
                
                # Extract text content using BeautifulSoup for cleaner text
                soup = BeautifulSoup(content, 'html.parser')
                
                # Remove script and style elements
                for script in soup(["script", "style"]):
                    script.decompose()
                
                text_content = soup.get_text(strip=True)
                
                if len(text_content) > 100:  # Ensure meaningful content
                    doc = self._create_document(url, text_content, "custom_requests")
                    documents.append(doc)
                    self.logger.info(f"✅ Successfully loaded: {url} ({len(text_content)} chars)")
                else:
                    self.logger.warning(f"⚠️ Insufficient content from: {url}")
                
            except Exception as e:
                self.logger.error(f"❌ Failed to load {url}: {e}")
        
        return documents
    
    def load_with_beautifulsoup(self) -> List[Document]:
        """
        Load content using BeautifulSoup with enhanced text extraction.
        
        Returns:
            List of LangChain Document objects
        """
        self.logger.info("🔄 Loading content with BeautifulSoup method")
        
        documents = []
        
        for url in self.urls:
            try:
                self.logger.info(f"🔄 Scraping: {url}")
                
                response = requests.get(
                    url,
                    headers=self.headers,
                    verify=False,
                    timeout=30
                )
                response.raise_for_status()
                
                soup = BeautifulSoup(response.content, 'html.parser')
                
                # Remove unwanted elements
                for element in soup(["script", "style", "nav", "footer", "header", "aside"]):
                    element.decompose()
                
                # Try to find main content areas
                main_content = soup.find('main') or soup.find('article') or soup.find('div', class_='content')
                
                if main_content:
                    text_content = main_content.get_text(strip=True, separator=' ')
                else:
                    text_content = soup.get_text(strip=True, separator=' ')
                
                # Clean up extra whitespace
                text_content = ' '.join(text_content.split())
                
                if len(text_content) > 100 and not self._is_content_blocked(text_content):
                    doc = self._create_document(url, text_content, "beautifulsoup")
                    documents.append(doc)
                    self.logger.info(f"✅ Successfully scraped: {url} ({len(text_content)} chars)")
                else:
                    self.logger.warning(f"⚠️ Insufficient or blocked content from: {url}")
                
            except Exception as e:
                self.logger.error(f"❌ Failed to scrape {url}: {e}")
        
        return documents
    
    def load_with_selenium(self, headless: bool = True, wait_time: int = 5) -> List[Document]:
        """
        Load content using Selenium WebDriver for JavaScript-heavy sites.
        
        Args:
            headless: Run browser in headless mode
            wait_time: Time to wait for page load
            
        Returns:
            List of LangChain Document objects
        """
        self.logger.info("🔄 Loading content with Selenium method")
        
        # Setup Chrome options
        chrome_options = Options()
        if headless:
            chrome_options.add_argument("--headless")
        chrome_options.add_argument("--no-sandbox")
        chrome_options.add_argument("--disable-dev-shm-usage")
        chrome_options.add_argument("--disable-gpu")
        chrome_options.add_argument("--window-size=1920,1080")
        chrome_options.add_argument(f"--user-agent={self.headers['User-Agent']}")
        chrome_options.add_argument("--ignore-certificate-errors")
        chrome_options.add_argument("--ignore-ssl-errors")
        chrome_options.add_argument("--disable-blink-features=AutomationControlled")
        chrome_options.add_experimental_option("excludeSwitches", ["enable-automation"])
        chrome_options.add_experimental_option('useAutomationExtension', False)
        
        documents = []
        driver = None
        
        try:
            driver = webdriver.Chrome(options=chrome_options)
            driver.execute_script("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})")
            
            for url in self.urls:
                try:
                    self.logger.info(f"🔄 Loading with Selenium: {url}")
                    driver.get(url)
                    
                    # Wait for page to load
                    time.sleep(wait_time)
                    
                    # Try to wait for specific elements to load
                    try:
                        WebDriverWait(driver, 10).until(
                            EC.presence_of_element_located((By.TAG_NAME, "body"))
                        )
                    except:
                        pass
                    
                    # Get page source and extract text
                    page_source = driver.page_source
                    soup = BeautifulSoup(page_source, 'html.parser')
                    
                    # Remove unwanted elements
                    for element in soup(["script", "style", "nav", "footer", "header", "aside"]):
                        element.decompose()
                    
                    text_content = soup.get_text(strip=True, separator=' ')
                    text_content = ' '.join(text_content.split())
                    
                    if len(text_content) > 100 and not self._is_content_blocked(text_content):
                        doc = self._create_document(url, text_content, "selenium")
                        documents.append(doc)
                        self.logger.info(f"✅ Successfully loaded: {url} ({len(text_content)} chars)")
                    else:
                        self.logger.warning(f"⚠️ Insufficient or blocked content from: {url}")
                    
                except Exception as e:
                    self.logger.error(f"❌ Failed to load {url} with Selenium: {e}")
        
        except Exception as e:
            self.logger.error(f"❌ Selenium setup failed: {e}")
        
        finally:
            if driver:
                driver.quit()
        
        return documents
    
    def load_with_unstructured(self) -> List[Document]:
        """
        Load content using UnstructuredURLLoader (original method with enhancements).
        
        Returns:
            List of LangChain Document objects
        """
        self.logger.info("🔄 Loading content with UnstructuredURLLoader method")
        
        documents = []
        
        # Try loading all URLs at once first
        try:
            loader = UnstructuredURLLoader(urls=self.urls, headers=self.headers)
            data = loader.load()
            
            for doc in data:
                content = doc.page_content.strip() if doc.page_content else ""
                if len(content) > 100 and not self._is_content_blocked(content):
                    doc.metadata['method'] = 'unstructured_bulk'
                    documents.append(doc)
                    self.logger.info(f"✅ Loaded via UnstructuredURLLoader: {doc.metadata.get('source', 'Unknown')}")
        
        except Exception as e:
            self.logger.warning(f"⚠️ Bulk loading failed: {e}")
        
        # Try loading individually for failed URLs
        loaded_sources = {doc.metadata.get('source') for doc in documents}
        remaining_urls = [url for url in self.urls if url not in loaded_sources]
        
        for url in remaining_urls:
            try:
                loader = UnstructuredURLLoader(urls=[url], headers=self.headers)
                data = loader.load()
                
                if data:
                    doc = data[0]
                    content = doc.page_content.strip() if doc.page_content else ""
                    if len(content) > 100 and not self._is_content_blocked(content):
                        doc.metadata['method'] = 'unstructured_individual'
                        documents.append(doc)
                        self.logger.info(f"✅ Loaded individually: {url}")
            
            except Exception as e:
                self.logger.error(f"❌ Failed to load {url} individually: {e}")
        
        return documents
    
    def _is_content_blocked(self, content: str) -> bool:
        """Check if content appears to be blocked or minimal."""
        blocked_indicators = [
            "blocked", "sorry, you have been blocked", "access denied",
            "please enable cookies", "security service", "cloudflare",
            "captcha", "rate limited"
        ]
        content_lower = content.lower()
        return any(indicator in content_lower for indicator in blocked_indicators) or len(content) < 100
    
    def load_all_methods(self) -> List[Document]:
        """
        Try all loading methods and return the best results.
        
        Returns:
            List of LangChain Document objects from the most successful method
        """
        self.logger.info("🔄 Trying all loading methods")
        
        methods = [
            ("UnstructuredURLLoader", self.load_with_unstructured),
            ("Custom Requests", self.load_with_custom_requests),
            ("BeautifulSoup", self.load_with_beautifulsoup),
            ("Selenium", self.load_with_selenium)
        ]
        
        best_documents = []
        best_method = ""
        
        for method_name, method_func in methods:
            try:
                self.logger.info(f"\n{'='*50}")
                self.logger.info(f"Trying method: {method_name}")
                self.logger.info(f"{'='*50}")
                
                documents = method_func()
                
                if len(documents) > len(best_documents):
                    best_documents = documents
                    best_method = method_name
                
                # If we got all URLs successfully, stop trying
                if len(documents) == len(self.urls):
                    self.logger.info(f"🎯 All URLs loaded successfully with {method_name}")
                    break
                    
            except Exception as e:
                self.logger.error(f"❌ Method {method_name} failed: {e}")
        
        self.logger.info(f"\n🏆 Best method: {best_method} with {len(best_documents)} documents")
        return best_documents
    
    def create_embeddings_pipeline(self, documents: List[Document], 
                                 chunk_size: int = 1000, 
                                 chunk_overlap: int = 200,
                                 model_name: str = "all-mpnet-base-v2") -> FAISS:
        """
        Complete pipeline: split documents and create FAISS vector store.
        
        Args:
            documents: List of Document objects
            chunk_size: Size of text chunks
            chunk_overlap: Overlap between chunks
            model_name: Sentence transformer model name
            
        Returns:
            FAISS vector store
        """
        if not documents:
            raise ValueError("No documents provided for embedding pipeline")
        
        self.logger.info(f"🔄 Creating embeddings pipeline with {len(documents)} documents")
        
        # Split documents
        text_splitter = RecursiveCharacterTextSplitter(
            chunk_size=chunk_size,
            chunk_overlap=chunk_overlap
        )
        docs = text_splitter.split_documents(documents)
        self.logger.info(f"📄 Split into {len(docs)} chunks")
        
        # Load and wrap the model
        self.logger.info(f"🤖 Loading embedding model: {model_name}")
        model = SentenceTransformer(model_name)
        embedding_model = SentenceTransformerEmbeddings(model_name=model_name)
        
        # Create FAISS vector store
        self.logger.info("🔗 Creating FAISS vector store")
        vectorstore = FAISS.from_documents(docs, embedding_model)
        
        self.logger.info("✅ Embedding pipeline completed successfully")
        return vectorstore

# Example usage and testing
if __name__ == "__main__":
    # Example URLs
    urls = [
        "https://nation.africa/kenya/news",
        "https://www.president.go.ke/administration/office-of-the-first-lady/",
        "https://www.standardmedia.co.ke/",  # Alternative source
    ]
    
    # Initialize the loader
    loader = WebContentLoader(urls)
    
    # Try all methods and get the best results
    documents = loader.load_all_methods()
    
    if documents:
        print(f"\n🎉 Successfully loaded {len(documents)} documents")
        
        # Show document details
        for i, doc in enumerate(documents):
            print(f"\nDocument {i+1}:")
            print(f"  Source: {doc.metadata.get('source', 'Unknown')}")
            print(f"  Method: {doc.metadata.get('method', 'Unknown')}")
            print(f"  Length: {len(doc.page_content)} characters")
            print(f"  Preview: {doc.page_content[:200]}...")
        
        # Create embeddings pipeline
        try:
            vectorstore = loader.create_embeddings_pipeline(documents)
            print(f"\n🎯 Successfully created vector store with {vectorstore.index.ntotal} embeddings")
            
            # Test similarity search
            query = "government news Kenya"
            results = vectorstore.similarity_search(query, k=2)
            print(f"\n🔍 Test query '{query}' returned {len(results)} results")
            
        except Exception as e:
            print(f"\n❌ Failed to create embeddings: {e}")
    
    else:
        print("\n❌ No documents were successfully loaded")
        print("💡 Consider:")
        print("  - Checking your internet connection")
        print("  - Trying different URLs")
        print("  - Using a VPN if blocked regionally")
        print("  - Installing ChromeDriver for Selenium method")