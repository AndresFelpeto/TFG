from VideoAnalyzer import analyze_video
from Progress import Progress
import os

def main():
    # Ruta fija al video que quieres analizar
    video_path = "videos_recibidos/Video_18.mp4"

    if not os.path.exists(video_path):
        print(f"❌ El archivo no existe: {video_path}")
        return

    print(f"📥 Analizando video: {video_path}")
    progress_main = Progress()
    progress_steps = Progress()

    out_video, angle_left, angle_right, zip_path = analyze_video(video_path, progress_main, progress_steps)

    print(f"\n✅ Procesamiento completado:")
    print(f"🎞️ Video generado: {out_video}")
    print(f"📈 Ángulo pie izquierdo: {angle_left:.2f}°")
    print(f"📈 Ángulo pie derecho: {angle_right:.2f}°")
    print(f"🖼️ ZIP con fotogramas clave: {zip_path}")
    print("📄 CSV de ángulos generado en la misma carpeta del video procesado.")

if __name__ == "__main__":
    main()
