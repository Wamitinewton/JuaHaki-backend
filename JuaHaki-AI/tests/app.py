from flask import Flask, jsonify, request

from scrapper.mainsc import get_answer, get_answer_with_specific_loader,load_content, load_specific_tool

app = Flask(__name__)

@app.route('/load_content', methods=['POST'])
def load_content_route():
    """
    Route to load content from provided URLs.
    """
    urls = request.get_json()
    if not urls:
        return jsonify({"error": "Invalid request. JSON body is required."}), 400
    response = load_content(urls)
    return jsonify(response)

@app.route('/load_specific_tool', methods=['POST'])
def load_specific_tool_route():
    """
    Route to load web content using a specific loader type.
    """
    data = request.get_json()
    if not data:
        return jsonify({"error": "Invalid request. JSON body is required."}), 400

    urls = data.get("urls", [])
    if not urls:
        return jsonify({"error": "Invalid request. 'urls' are required."}), 400
    loader_type = data.get("loader_type", "custom")
    if not loader_type:
        return jsonify({"error": "Invalid request. 'loader_type' is required."}), 400
    
    response = load_specific_tool(urls,data)
    return jsonify(response)

@app.route('/get_answer', methods=['POST'])
def get_answer_route():
    """
    Route to get an answer based on the provided question.
    """
    data = request.get_json()
    if not data:
        return jsonify({"error": "Invalid request. JSON body is required."}), 400

    question = data.get("question", "")
    if not question:
        return jsonify({"error": "Invalid request. 'question' is required."}), 400
    
    response = get_answer(question)
    return jsonify(response)


@app.route('/get_answer_with_specific_loader', methods=['POST'])
def get_answer_with_specific_loader_route():
    """
    Route to get an answer using a specific loader type.
    """
    data = request.get_json()
    if not data:
        return jsonify({"error": "Invalid request. JSON body is required."}), 400

    question = data.get("question", "")
    if not question:
        return jsonify({"error": "Invalid request. 'question' is required."}), 400
    
    loader_type = data.get("loader_type", "custom")
    if not loader_type:
        return jsonify({"error": "Invalid request. 'loader_type' is required."}), 400
    
    response = get_answer_with_specific_loader(question)
    return jsonify(response)

if __name__ == '__main__':
    app.run(debug=True)