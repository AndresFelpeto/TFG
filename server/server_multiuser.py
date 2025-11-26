import secrets
from flask import Flask, request, jsonify, send_file
import os
from datetime import datetime
import threading
from Progress import Progress
from VideoAnalyzer import analyze_video

app = Flask(__name__)
procesos = {}
UPLOAD_FOLDER = "videos_recibidos"
os.makedirs(UPLOAD_FOLDER, exist_ok=True)


def procesar_video(token, filepath):
    try:
        step_progress = procesos[token]["progress_step"]
        analyzer_progress = procesos[token]["progress_analyzer"]
        processed_video_path, angle_left_foot, angle_right_foot, frames_path = analyze_video(filepath, analyzer_progress, step_progress)
        procesos[token]["status"] = "done"
        procesos[token]["output"] = processed_video_path
        procesos[token]["angles"] = {
            "left": angle_left_foot,
            "right": angle_right_foot
        }
        procesos[token]["frames_zip"] = frames_path
        print(f"Análisis completado para token: {token}")
    except Exception as e:
        procesos[token]["status"] = "error"
        print(f"Error procesando token {token}: {e}")


@app.route("/upload_video", methods=["POST"])
def upload_video():
    token = secrets.token_hex(16)
    progress_step = Progress()
    progress_analyzer = Progress()
    procesos[token] = {
        "status": "processing",
        "percent_completed": 1,
        "output": None,
        "angles": None,
        "frames_zip": None,
        "progress_step": progress_step,
        "progress_analyzer": progress_analyzer
    }

    if "video" not in request.files:
        return jsonify({"status": "error", "message": "No se recibió el archivo 'video'"}), 400

    file = request.files["video"]
    filename = datetime.now().strftime("video_%Y%m%d_%H%M%S.mp4")
    filepath = os.path.join(UPLOAD_FOLDER, filename)
    file.save(filepath)

    threading.Thread(target=procesar_video, args=(token, filepath)).start()
    return jsonify({
        "status": "ok",
        "token": token,
        "message": "Video recibido, procesando"
    }), 200


@app.route("/request_video", methods=["GET"])
def send_video():
    token = request.args.get("token")
    if not token or token not in procesos:
        return jsonify({"status": "error", "message": "Token inválido"}), 400

    if procesos[token]["status"] == "error":
        return jsonify({"status": "error", "message": "Fallo desconocido"}), 500

    if procesos[token]["status"] != "done" or not procesos[token].get("output"):
        percent_completed = (
            procesos[token]["progress_analyzer"].percent_completed() +
            procesos[token]["progress_step"].percent_completed()
        ) / 2
        return jsonify({"status": "processing", "remaining": int(percent_completed)}), 200

    processed_video_path = procesos[token]["output"]
    if not os.path.exists(processed_video_path):
        return jsonify({"status": "error", "message": "El video procesado no se encuentra"}), 500

    print(f"🎥 Enviando video procesado: {processed_video_path}")
    return send_file(processed_video_path, mimetype="video/mp4"), 200


@app.route("/get_results", methods=["GET"])
def get_pisada():
    token = request.args.get("token")
    if not token or token not in procesos:
        return jsonify({"status": "error", "message": "Token inválido"}), 400

    if procesos[token]["status"] != "done":
        return jsonify({"status": "error", "message": "Procesamiento no completado"}), 400

    print(f"🔓 Enviando datos de pisada para token: {token}")
    return jsonify({
        "angle_left_foot": procesos[token]["angles"]["left"],
        "angle_right_foot": procesos[token]["angles"]["right"]
    }), 200


@app.route("/get_frames_zip", methods=["GET"])
def get_frames_zip():
    token = request.args.get("token")
    if not token or token not in procesos:
        return jsonify({"status": "error", "message": "Token inválido"}), 400

    info = procesos[token]
    if info["status"] != "done" or not info.get("frames_zip"):
        return jsonify({"status": "error", "message": "Frames no disponibles aún"}), 400

    zip_path = info["frames_zip"]
    if not os.path.exists(zip_path):
        return jsonify({"status": "error", "message": "El archivo ZIP no se encuentra"}), 500

    print(f"📦 Enviando ZIP de frames: {zip_path}")
    return send_file(zip_path, mimetype="application/zip", as_attachment=True), 200


"""if __name__ == "__main__":
    app.run(host="0.0.0.0",
            port=443,
            ssl_context=(
                "/etc/letsencrypt/live/andrestfg.es/fullchain.pem",
                "/etc/letsencrypt/live/andrestfg.es/privkey.pem"
            ),
            debug=True)
            """
if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=True)
