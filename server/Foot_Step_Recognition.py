import cv2
import mediapipe as mp
from Progress import Progress


def foot_step_frames(video_path, progress: Progress | None = None):
    mp_pose = mp.solutions.pose
    pose = mp_pose.Pose(static_image_mode=False)

    cap = cv2.VideoCapture(video_path)
    if not cap.isOpened():
        pose.close()
        raise FileNotFoundError(f"No se pudo abrir el video: {video_path}")

    if progress is None:
        progress = Progress()

    height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
    
    total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT)) or 1
    progress.total = total_frames
    progress.current = 0
    progress.done = False

    frame_idx = 0

    # Parámetros de detección
    umbral_subida = 3
    margen_altura = 20

    # Talón derecho
    prev_heel_y_d = None
    max_heel_y_d = 0.0
    frames_ignorados_d = 0
    pisadas_detectadas_d = []

    # Talón izquierdo
    prev_heel_y_i = None
    max_heel_y_i = 0.0
    frames_ignorados_i = 0
    pisadas_detectadas_i = []

    # Para guardar las alturas por frame
    altura_heel_d_por_frame = {}
    altura_heel_i_por_frame = {}

    while True:
        ret, frame = cap.read()
        if not ret:
            break

        frame_rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        results = pose.process(frame_rgb)

        if results.pose_landmarks:
            lm = results.pose_landmarks.landmark

            # Talón derecho
            heel_d = lm[mp_pose.PoseLandmark.RIGHT_HEEL]
            heel_y_d = heel_d.y * height
            altura_heel_d_por_frame[frame_idx] = heel_y_d

            if heel_y_d > max_heel_y_d:
                max_heel_y_d = heel_y_d

            if prev_heel_y_d is not None:
                hubo_subida_d = prev_heel_y_d - heel_y_d > umbral_subida
                cerca_suelo_d = abs(prev_heel_y_d - max_heel_y_d) < margen_altura

                if hubo_subida_d and cerca_suelo_d and frames_ignorados_d == 0:
                    pisadas_detectadas_d.append(frame_idx)
                    frames_ignorados_d = 7

            prev_heel_y_d = heel_y_d

            # Talón izquierdo
            heel_i = lm[mp_pose.PoseLandmark.LEFT_HEEL]
            heel_y_i = heel_i.y * height
            altura_heel_i_por_frame[frame_idx] = heel_y_i

            if heel_y_i > max_heel_y_i:
                max_heel_y_i = heel_y_i

            if prev_heel_y_i is not None:
                hubo_subida_i = prev_heel_y_i - heel_y_i > umbral_subida
                cerca_suelo_i = abs(prev_heel_y_i - max_heel_y_i) < margen_altura

                if hubo_subida_i and cerca_suelo_i and frames_ignorados_i == 0:
                    pisadas_detectadas_i.append(frame_idx)
                    frames_ignorados_i = 4

            prev_heel_y_i = heel_y_i

        if frames_ignorados_d > 0:
            frames_ignorados_d -= 1
        if frames_ignorados_i > 0:
            frames_ignorados_i -= 1

        frame_idx += 1
        progress.current = frame_idx  # <<< actualizar avance

    cap.release()
    pose.close()

    # Post-procesamiento: escoger el frame más bajo entre f, f-1, f-2
    def refinar_frames(lista_frames, alturas):
        pisadas_finales = []
        for f in lista_frames:
            candidatos = [f - 2, f - 1, f]
            candidatos_validos = [i for i in candidatos if i in alturas]
            if candidatos_validos:
                mejor_frame = max(candidatos_validos, key=lambda x: alturas[x])
                pisadas_finales.append(mejor_frame)
        return pisadas_finales

    pisadas_final_d=None
    pisadas_final_i=None
    pisadas_final_d = refinar_frames(pisadas_detectadas_d, altura_heel_d_por_frame)
    pisadas_final_i = refinar_frames(pisadas_detectadas_i, altura_heel_i_por_frame)

    progress.done = True
    progress.current = progress.total
    
    return pisadas_final_d, pisadas_final_i, progress
