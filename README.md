# Friends Facial Recognition

A Java application that uses computer vision and machine learning to recognize faces from a webcam feed and greet recognized individuals using voice synthesis.

## Overview

This project combines several technologies to create a facial recognition system:

- **OpenCV**: For computer vision and face detection
- **Face Recognition Model**: Uses a pre-trained ONNX model for face recognition
- **Google Cloud Text-to-Speech API**: For voice synthesis to greet recognized individuals

The application captures video from a webcam, detects faces in the video stream, recognizes known faces, and greets recognized individuals using voice synthesis.

## Features

- **Real-time Face Detection**: Detects faces in the webcam feed using Haar cascades
- **Face Recognition**: Recognizes known faces using a pre-trained model
- **Voice Synthesis**: Greets recognized individuals using Google Cloud Text-to-Speech
- **Database Management**: Save and load face databases for persistent recognition
- **User Interface**: Simple keyboard-based interface for interaction

## Requirements

- Java 21 or higher
- Maven
- Webcam
- Google Cloud API Key (for voice synthesis)

## Installation

1. Clone the repository:
   ```
   git clone https://github.com/yourusername/FriendsFacialRecognition.git
   cd FriendsFacialRecognition
   ```

2. Build the project with Maven:
   ```
   mvn clean package
   ```

3. Set up Google Cloud API Key:
   - Create a Google Cloud account and project
   - Enable the Text-to-Speech API
   - Create an API key
   - Update the API key in `VoiceSynthesizer.java`

## Usage

Run the application:

```
java -jar target/FriendsFacialRecognition-1.0-SNAPSHOT.jar
```

### Command Line Options

- `--no-speech` or `-ns`: Disable voice synthesis
- `--help` or `-h`: Display help information

### Keyboard Controls

While the application is running:

- **A**: Add a new face to the database
- **L**: Load a face database from a file
- **S**: Save the current face database to a file
- **V**: Toggle voice synthesis on/off
- **Space**: Confirm adding a face (when in add mode)
- **Esc**: Exit the application

## Project Structure

- `src/main/java/fr/anisikram/`
  - `Main.java`: Main application entry point
  - `VoiceSynthesizer.java`: Handles voice synthesis using Google Cloud
  - `GoogleCloudApiKeyCredentials.java`: Manages Google Cloud authentication
  - `faces/`
    - `FaceDetector.java`: Detects faces in images using Haar cascades
    - `FaceRecognizer.java`: Recognizes faces using a pre-trained model
  - `video/`
    - `VideoCapturer.java`: Captures video from the webcam

- `models/`
  - `face_recognition_sface_2021dec.onnx`: Pre-trained face recognition model

- `src/main/resources/`
  - `haarcascades/haarcascade_frontalface_default.xml`: Haar cascade for face detection

## How It Works

1. The application captures frames from the webcam
2. Each frame is processed to detect faces using Haar cascades
3. Detected faces are extracted and processed for recognition
4. The face recognition model compares the face with known faces in the database
5. If a match is found, the person is greeted using voice synthesis
6. The user can add new faces to the database, save/load databases, and toggle features

## License

[Specify your license here]

## Acknowledgments

- OpenCV for computer vision capabilities
- Google Cloud for Text-to-Speech API
- [Any other acknowledgments]