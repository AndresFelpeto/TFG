import uuid
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
remaining=1
def procesar_video(pid, filepath):
    try:
        step_progress = procesos[pid]["progress_step"]
        analyzer_progress = procesos[pid]["progress_analyzer"]
        processed_video_path, angle_left_foot, angle_right_foot, frames_path = analyze_video(filepath,analyzer_progress,step_progress)
        procesos[pid]["status"] = "done"
        procesos[pid]["output"] = processed_video_path
        procesos[pid]["angles"] = {
            "left": angle_left_foot,
            "right": angle_right_foot
        }
        procesos[pid]["frames_zip"] = frames_path
        print(f"Análisis completado para {pid}")
    except Exception as e:
        procesos[pid]["status"] = "error"
        procesos[pid]["error"] = "Error procesando el video"
        print(f"Error procesando {pid}: {e}")

@app.route("/upload_video", methods=["POST"])
def upload_video():
    pid = str(uuid.uuid4())
    progress_step = Progress()
    progress_analyzer = Progress()
    procesos[pid] = {
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
    if file.filename == "":
        return jsonify({"status": "error", "message": "El archivo no tiene nombre"}), 400

    filename = datetime.now().strftime("video_%Y%m%d_%H%M%S.mp4")
    filepath = os.path.join(UPLOAD_FOLDER, filename)
    file.save(filepath)

    
    threading.Thread(target=procesar_video, args=(pid, filepath)).start()
    return jsonify({
        "status": "ok",
        "process_id": pid,
        "message": "Video recibido, procesando"
    }), 200

@app.route("/request_video", methods=["GET"])
def send_video():
    pid = request.args.get("process_id")
    if not pid or pid not in procesos:
        return jsonify({"status": "error", "message": "ID inválido"}), 400

    procesos[pid]
    
    if procesos["status"] == "error":
        return jsonify({"status": "error", "message": procesos.get("error", "Fallo desconocido")}), 500

    if procesos["status"] != "done" or not procesos.get("output"):
        percent_completed = (procesos["progress_analyzer"].percent_completed() + procesos["progress_step"].percent_completed()) / 2
        return jsonify({"status": "processing", "remaining": int(percent_completed)}), 200

    processed_video_path = procesos["output"]
    if not os.path.exists(processed_video_path):
        return jsonify({"status": "error", "message": "El video procesado no se encuentra"}), 500

    print(f"🎥 Enviando video procesado: {processed_video_path}")
    return send_file(processed_video_path, mimetype="video/mp4")

@app.route("/get_results", methods=["GET"])
def get_pisada():
    pid = request.args.get("process_id")
    if not pid or pid not in procesos:
        return jsonify({"status": "error", "message": "ID inválido"}), 400

    if procesos[pid]["status"] != "done":
        return jsonify({"status": "error", "message": "Procesamiento no completado"}), 400

    print(f"🔓 Enviando datos de pisada para {pid}")
    return jsonify({
        "angle_left_foot": procesos[pid]["angles"]["left"],
        "angle_right_foot": procesos[pid]["angles"]["right"]
    })

@app.route("/get_frames_zip", methods=["GET"])
def get_frames_zip():
    pid = request.args.get("process_id")
    if not pid or pid not in procesos:
        return jsonify({"status": "error", "message": "ID inválido"}), 400

    info = procesos[pid]

    if info["status"] != "done" or not info.get("frames_zip"):
        return jsonify({"status": "error", "message": "Frames no disponibles aún"}), 400

    zip_path = info["frames_zip"]
    if not os.path.exists(zip_path):
        return jsonify({"status": "error", "message": "El archivo ZIP no se encuentra"}), 500

    print(f"📦 Enviando ZIP de frames: {zip_path}")
    return send_file(zip_path, mimetype="application/zip", as_attachment=True)

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=True)
