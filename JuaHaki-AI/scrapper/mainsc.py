from route import load_web_content, split_documents, create_vector_store, initialize_llm, create_prompt_template, extract_content,load_vector_store,load_with_specific_loader
from langchain.chains import RetrievalQA
from web_scrapper import WebContentLoader


def load_content(request):
    """
    Load content from the provided URLs and save it to the vector store.
    
    Args:
        request (dict): A dictionary containing 'urls' (list of URLs).
    
    Returns:
        dict: A dictionary indicating success or failure.
    """
    urls = request.get("urls", [])
    
    if not urls:
        return {"error": "Invalid request. 'urls' are required."}
    
    documents = load_web_content(urls)
    split_docs = split_documents(documents)
    create_vector_store(split_docs)
    
    return {"message": "Content loaded successfully."}

def load_specific_tool(request):
    """
    Load web content using a specific loader type and return the documents.
    
    Args:
        request (dict): A dictionary containing 'url' (str) and 'loader_type' (str).
    
    Returns:
        dict: A dictionary containing the loaded documents.
    """
    url = request.get("url", "")
    loader_type = request.get("loader_type", "custom")
    
    if not url:
        return {"error": "Invalid request. 'url' is required."}
    
    documents = load_with_specific_loader(url, loader_type)
    split_docs = split_documents(documents)
    vector_store = create_vector_store(split_docs)
    return {"message": f"Content loaded successfully with specific loader {loader_type}."}
    
    

def get_answer(request):
    """
    Process a request to get an answer and sources based on the provided question.
    
    Args:
        request (dict): A dictionary containing 'question' (str).
    
    Returns:
        dict: A dictionary containing the answer and sources.
    """
    question = request.get("question", "")
    
    if not question:
        return {"error": "Invalid request. 'question' is required."}
    
    vector_store = load_vector_store()
    retriever = vector_store.as_retriever(search_kwargs={"k": 3})
    
    llm = initialize_llm()
    prompt = create_prompt_template()
    qa_chain = RetrievalQA.from_chain_type(
        llm=llm,
        chain_type="stuff",
        retriever=retriever,
        chain_type_kwargs={"prompt": prompt},
        return_source_documents=True
    )
    
    result = qa_chain({"query": question})
    return extract_content(result)

def get_answer_with_specific_loader(request):
    """
    Process a request to get an answer using a specific loader type.
    
    Args:
        request (dict): A dictionary containing 'url' (str), 'loader_type' (str), and 'question' (str).
    
    Returns:
        dict: A dictionary containing the answer and sources.
    """
   
    question = request.get("question", "")
    
    if not question:
        return {"error": "Invalid request. 'url' and 'question' are required."}
    
   
    vector_store = load_vector_store()
    retriever = vector_store.as_retriever(search_kwargs={"k": 3})
    
    llm = initialize_llm()
    prompt = create_prompt_template()
    qa_chain = RetrievalQA.from_chain_type(
        llm=llm,
        chain_type="stuff",
        retriever=retriever,
        chain_type_kwargs={"prompt": prompt},
        return_source_documents=True
    )
    
    result = qa_chain({"query": question})
    return extract_content(result)