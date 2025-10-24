# Version de Python
FROM python:3.10-slim
# Establecer directorio de trabajo
WORKDIR /app
# Copiar los archivos del proyecto
COPY . /app
# Instalar dependencias del sistema necesarias para OpenCV
RUN apt-get update && apt-get install -y --no-install-recommends \
    libgl1 \
    libglib2.0-0 \
    libsm6 \
    libxext6 \
    libxrender1 \
    certbot && \
    rm -rf /var/lib/apt/lists/*
# Instalar dependencias de Python
RUN pip install --no-cache-dir -r requirements.txt
# Exponer el puerto HTTPS
EXPOSE 443
# Comando para ejecutar Flask
CMD ["python", "server_multiuser.py"]

