import cv2
import mediapipe as mp
import numpy as np
import os
from Foot_Step_Recognition import foot_step_frames,ProgressStep

class ProgressAnalyzer:
    def __init__(self):
        self.total = 0
        self.current = 0
        self.done = False

    def remaining_frames(self) -> int:
        return max(0, self.total - self.current)

    def remaining_percent(self) -> int:
        if self.total <= 0:
            return 100 if not self.done else 0
        return max(0, 100 - int((self.current / self.total) * 100))

def calculate_angle(ankle, knee, lado='izquierdo'):
    vec_leg = np.array([ankle[0] - knee[0], ankle[1] - knee[1]])
    vertical = np.array([0, 1])
    vec_leg_norm = vec_leg / np.linalg.norm(vec_leg)
    dot = np.dot(vec_leg_norm, vertical)
    dot = np.clip(dot, -1.0, 1.0)
    angle_rad = np.arccos(dot)
    angle_deg = np.degrees(angle_rad)
    cross = vec_leg_norm[0] * vertical[1] - vec_leg_norm[1] * vertical[0]
    signed_angle = angle_deg if (cross > 0 if lado == 'izquierdo' else cross < 0) else -angle_deg
    return signed_angle

def classify_rearfoot_angle_label(angle):
    angle = abs(angle)
    if angle > 7:
        return "Pronado"
    elif angle < 3.5:
        return "Supinado"
    else:
        return "Neutro"

def mean_data(valores):
    if not valores:
        return None
    valores_ordenados = sorted(valores)
    n = len(valores_ordenados)
    n_20 = max(1, int(n * 0.2))
    inicio = (n - n_20) // 2
    fin = inicio + n_20
    central_20 = valores_ordenados[inicio:fin]
    return np.mean(central_20)

def analyze_video(video_path, progress: ProgressAnalyzer | None = None,progress_step: ProgressStep | None = None):
    mp_pose = mp.solutions.pose
    pose = mp_pose.Pose()
    if progress_step is None:
        progress_step=ProgressStep()
    pisadas_final_d, pisadas_final_i,_ = foot_step_frames(video_path,progress_step)

    cap = cv2.VideoCapture(video_path)
    fourcc = cv2.VideoWriter_fourcc(*'mp4v')
    os.makedirs("videos_resultado", exist_ok=True)
    out_path = os.path.join("videos_resultado", os.path.basename(video_path))

    if progress is None:
        progress = ProgressAnalyzer()

    total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT)) or 1
    progress.total = total_frames
    progress.current = 0
    progress.done = False

    width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
    height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
    fps = cap.get(cv2.CAP_PROP_FPS)
    writer = cv2.VideoWriter(out_path, fourcc, fps, (width, height))

    frame_idx = 0
    ground_level_fixed = 0
    left_angles = []
    right_angles = []

    while cap.isOpened():
        ret, frame = cap.read()
        if not ret:
            break

        frame_rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        results = pose.process(frame_rgb)

        if results.pose_landmarks:
            h, w, _ = frame.shape
            lm = results.pose_landmarks.landmark

            knee_left = lm[25]; ankle_left = lm[27]; heel_left = lm[29]
            knee_right = lm[26]; ankle_right = lm[28]; heel_right = lm[30]

            # Coordenadas absolutas
            x_kl, y_kl = int(knee_left.x * w), int(knee_left.y * h)
            x_kr, y_kr = int(knee_right.x * w), int(knee_right.y * h)
            x_al, y_al = int(ankle_left.x * w), int(ankle_left.y * h)
            x_ar, y_ar = int(ankle_right.x * w), int(ankle_right.y * h)
            x_hl, y_hl = int(heel_left.x * w), int(heel_left.y * h)
            x_hr, y_hr = int(heel_right.x * w), int(heel_right.y * h)

            # Suelo dinámico
            y_positions = [y_al, y_ar, y_hl, y_hr]
            ground_level = max(y_positions) + 5
            if ground_level > ground_level_fixed:
                ground_level_fixed = ground_level

            # Dibujar líneas y puntos
            cv2.line(frame, (x_kl, y_kl), (x_al, y_al), (0, 0, 0), 2)
            cv2.line(frame, (x_kr, y_kr), (x_ar, y_ar), (0, 0, 0), 2)
            for x, y, color in [
                (x_al, y_al, (255, 255, 255)), (x_ar, y_ar, (255, 255, 255)),
                (x_kl, y_kl, (255, 255, 255)), (x_kr, y_kr, (255, 255, 255)),
                (x_hl, y_hl, (255, 255, 255)), (x_hr, y_hr, (255, 255, 255))
            ]:
                cv2.circle(frame, (x, y), 6, color, -1)

            # Si el frame está en las listas de pisadas
            if frame_idx in pisadas_final_i:
                angle = calculate_angle((x_al, y_al), (x_kl, y_kl), lado='izquierdo')
                left_angles.append(angle)
                cv2.putText(frame, f"{angle:.1f}°", (x_al - 100, y_al - 30),
                            cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 255, 0), 2)

            if frame_idx in pisadas_final_d:
                angle = calculate_angle((x_ar, y_ar), (x_kr, y_kr), lado='derecho')
                right_angles.append(angle)
                cv2.putText(frame, f"{angle:.1f}°", (x_ar + 10, y_ar - 30),
                            cv2.FONT_HERSHEY_SIMPLEX, 0.6, (255, 0, 0), 2)

        writer.write(frame)
        frame_idx += 1
        progress.current = frame_idx  # <<< actualizar avance

    cap.release()
    writer.release()

    # Cálculo de medias y clasificación
    angle_left_foot = None
    angle_right_foot = None

    if left_angles:
        avg_left = mean_data(left_angles)
        angle_left_foot = classify_rearfoot_angle_label(avg_left)
        print(f"Media pie izquierdo: {avg_left:.2f}° - {angle_left_foot}")

    if right_angles:
        avg_right = mean_data(right_angles)
        angle_right_foot = classify_rearfoot_angle_label(avg_right)
        print(f"Media pie derecho: {avg_right:.2f}° - {angle_right_foot}")

    print(f"🎞️ Video generado con ángulos de pisada: {out_path}")
    return out_path, avg_left, avg_right
