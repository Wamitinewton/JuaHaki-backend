from route import load_web_content, split_documents, create_vector_store, initialize_llm, create_prompt_template, extract_content,load_vector_store
from langchain.chains import RetrievalQA

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