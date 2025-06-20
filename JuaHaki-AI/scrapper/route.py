import pickle
import json
import os
from dotenv import load_dotenv
from langchain import OpenAI
from langchain.chains import RetrievalQA
from langchain.text_splitter import RecursiveCharacterTextSplitter
from langchain.prompts import PromptTemplate
from langchain.embeddings import OpenAIEmbeddings
from langchain.vectorstores import FAISS
from langchain.chat_models import ChatOpenAI
from web_scrapper import WebContentLoader


load_dotenv()

# Constants
FAISS_STORE_PATH = "faiss_store_openai.pkl"
PROMPT_TEMPLATE = """Use the following pieces of context to answer the question at the end. 
If you don't know the answer, just say that you don't know, don't try to make up an answer.
Always include the source URLs for the information you use in your answer.

{context}

Question: {question}
Helpful Answer:"""



def initialize_llm():
    """Initialize the ChatOpenAI model."""
    return ChatOpenAI(
        model_name="gpt-3.5-turbo",
        temperature=0.7,
        max_tokens=500,
        openai_api_key=os.getenv("OPENAI_API_KEY")
    )

def create_prompt_template():
    """Create a custom prompt template."""
    return PromptTemplate(
        template=PROMPT_TEMPLATE, input_variables=["context", "question"]
    )



def load_web_content(urls: list):
    """Load web content using WebContentLoader."""
    all_documents = []
    for url in urls:
        loader = WebContentLoader(url)
        all_documents.extend(loader.load_all_methods())
    return all_documents

def split_documents(documents):
    """Split documents into smaller chunks."""
    text_splitter = RecursiveCharacterTextSplitter(
        separators=['\n\n', '\n', '.', '!', '?', ','],
        chunk_size=1000, 
        chunk_overlap=200
    )
    return text_splitter.split_documents(documents)

def create_vector_store(split_docs):
    """Create and save a FAISS vector store."""
    embeddings = OpenAIEmbeddings()
    vector_store = FAISS.from_documents(split_docs, embeddings)
    save_vector_store(vector_store)
    return load_vector_store()

def save_vector_store(vector_store):
    """Save the FAISS vector store to a file."""
    with open(FAISS_STORE_PATH, "wb") as f:
        pickle.dump(vector_store, f)

def load_vector_store():
    """Load the FAISS vector store from a file."""
    with open(FAISS_STORE_PATH, "rb") as f:
        return pickle.load(f)
    

def scrape_web_content(url: str) -> json:
    """
    Scrape web content from the given URL and return it as JSON.
    
    Args:
        url (str): The URL to scrape.
    
    Returns:
        json: The scraped content in JSON format.
    """
    documents = load_web_content(url)
    split_docs = split_documents(documents)
    vector_store = create_vector_store(split_docs)
    retriever = vector_store.as_retriever(search_kwargs={"k": 3})
    content = run_qa_chain(retriever)
    return json.dumps(content, indent=2)

def run_qa_chain(retriever):
    """Run the QA chain and return the result."""
    llm = initialize_llm()
    prompt = create_prompt_template()
    qa_chain = RetrievalQA.from_chain_type(
        llm=llm,
        chain_type="stuff",
        retriever=retriever,
        chain_type_kwargs={"prompt": prompt},
        return_source_documents=True
    )
    query = "What is the main purpose of the content on this page?"
    result = qa_chain({"query": query})
    return extract_content(result)

def extract_content(result):
    """Extract the answer and sources from the QA chain result."""
    answer = result['result']
    sources = [doc.metadata['source'] for doc in result['source_documents']]
    return {
        "answer": answer,
        "sources": sources
    }

#load web content using either custom loader , langchain's web loader,beautiful soup or selenium
def load_with_specific_loader(url: str, loader_type: str = "custom") -> list:
    """
    Load web content using a specific loader type.
    
    Args:
        url (str): The URL to load.
        loader_type (str): The type of loader to use ("custom", "langchain", "beautiful_soup", "selenium").
    
    Returns:
        list: A list of documents loaded from the URL.
    """

    loader = WebContentLoader(url)
    if loader_type == "custom":
       
        return loader.load_with_custom_requests([url])
    elif loader_type == "selenium":
        return loader.load_with_selenium([url])
    elif loader_type == "beautiful_soup":
       return loader.load_with_beautifulsoup([url])
    elif loader_type == "langchain":
        return loader.load_with_unstructured([url])
    else:
        raise ValueError("Unsupported loader type.")