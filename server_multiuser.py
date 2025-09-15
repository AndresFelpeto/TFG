import uuid
from flask import Flask, request, jsonify, send_file
import os
from datetime import datetime
import threading
import time

from Foot_Step_Recognition import ProgressStep
from VideoAnalyzer import analyze_video, ProgressAnalyzer

app = Flask(__name__)
procesos = {}
UPLOAD_FOLDER = "videos_recibidos"
os.makedirs(UPLOAD_FOLDER, exist_ok=True)
remaining=1
def procesar_video(pid, filepath):
    try:
        step_progress = procesos[pid]["progress_step"]
        analyzer_progress = procesos[pid]["progress_analyzer"]
        processed_video_path, angle_left_foot, angle_right_foot = analyze_video(filepath,analyzer_progress,step_progress)
        procesos[pid]["status"] = "done"
        procesos[pid]["output"] = processed_video_path
        procesos[pid]["angles"] = {
            "left": angle_left_foot,
            "right": angle_right_foot
        }
        print(f"✅ Análisis completado para {pid}")
    except Exception as e:
        procesos[pid]["status"] = "error"
        procesos[pid]["error"] = str(e)
        print(f"❌ Error procesando {pid}: {e}")

@app.route("/upload", methods=["POST"])
def upload_video():
    pid = str(uuid.uuid4())
    progress_step = ProgressStep()
    progress_analyzer = ProgressAnalyzer()
    procesos[pid] = {
        "status": "processing",
        "remaining": 1,
        "output": None,
        "angles": None,
        "progress":0,
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

@app.route("/request", methods=["GET"])
def send_video():
    pid = request.args.get("process_id")
    if not pid or pid not in procesos:
        return jsonify({"status": "error", "message": "ID inválido"}), 400

    info = procesos[pid]
    
    if info["status"] == "error":
        return jsonify({"status": "error", "message": info.get("error", "Fallo desconocido")}), 500

    if info["status"] != "done" or not info.get("output"):
        # No bloquear; el cliente debe consultar /status y solo llamar aquí cuando esté done
        remaining_percent = (info["progress_analyzer"].remaining_percent() + info["progress_step"].remaining_percent()) / 2
        return jsonify({"status": "processing", "remaining": int(remaining_percent)}), 200

    processed_video_path = info["output"]
    if not os.path.exists(processed_video_path):
        return jsonify({"status": "error", "message": "El video procesado no se encuentra"}), 500

    print(f"🎥 Enviando video procesado: {processed_video_path}")
    return send_file(processed_video_path, mimetype="video/mp4")

@app.route("/get_pisada", methods=["GET"])
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

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=True)
