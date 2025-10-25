from VideoAnalyzer import analyze_video  # Asegúrate de que este módulo esté correctamente nombrado
import os

def main():
    nombre_video = "Video_prueba.mp4"  # Cambia esto al nombre real
    ruta_video = os.path.join("videos_recibidos", nombre_video)

    if not os.path.exists(ruta_video):
        print(f"❌ No se encontró el video: {ruta_video}")
        return

    print(f"🔍 Analizando video: {ruta_video}")
    video_out, angle_left, angle_right, zip_path = analyze_video(ruta_video)

    print(f"✅ Video procesado guardado en: {video_out}")
    print(f"👣 Ángulo izquierdo: {angle_left:.2f}°")
    print(f"👣 Ángulo derecho: {angle_right:.2f}°")
    print(f"🗂️ ZIP de frames clave en: {zip_path}")

if __name__ == "__main__":
    main()
