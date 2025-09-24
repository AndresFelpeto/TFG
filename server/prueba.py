import sys
from VideoAnalyzercopy import analyze_video 
from Progress import Progress


def main():
    # Ruta del video original (modifica según tu archivo)
    video_input_path = "videos_recibidos/video_20250915_122303.mp4"

    print(f"🎬 Analizando video: {video_input_path}")

    try:
        # Crear objetos de progreso (opcional)
        progress = Progress()
        progress_step = Progress()

        # Procesar el video
        output_path, avg_left, avg_right, frames = analyze_video(video_input_path, progress, progress_step)
        print(f"\n✅ Video procesado y guardado en: {output_path}")
        print(f"📊 Resultado medio - Pie Izquierdo: {avg_left:.2f}°")
        print(f"📊 Resultado medio - Pie Derecho: {avg_right:.2f}°")

    except Exception as e:
        print(f"❌ Error al procesar el video: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()