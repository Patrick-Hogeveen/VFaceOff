let stream: MediaStream | null = null;
let deviceInfo: MediaDeviceInfo[] = [];

function getStream (videoSelect: HTMLSelectElement) {
    if (stream) {
        stream.getTracks().forEach((track) => {
            track.stop();
        })
    }
    const videoSource = videoSelect.value;
    const constraints = {
        video: {
            deviceId: videoSource ? { exact: videoSource } : undefined,
            width: 640,
            height: 480,
            maxWidth: 640,
            maxHeight: 640
        }
    };
    return navigator.mediaDevices.getUserMedia(constraints);
}

function gotStream (stream: MediaStream, videoSelect: HTMLSelectElement, videoElement: HTMLVideoElement) {
    stream = stream;
    videoSelect.selectedIndex = [...videoSelect.options].findIndex(
        (option) => option.text === stream.getVideoTracks()[0].label
    );
    videoElement.srcObject = stream;
    
    return navigator.mediaDevices.enumerateDevices();
}

function gotDevices (mediaDeviceInfos: MediaDeviceInfo[]) {
    deviceInfo = mediaDeviceInfos.filter((d) => d.kind === 'videoinput');
    return deviceInfo;
}

export function setupCamera (videoElement: HTMLVideoElement, videoSelect: HTMLSelectElement): Promise<MediaDeviceInfo[]> {
    return getStream(videoSelect)
        .then(constraints => gotStream(constraints, videoSelect, videoElement))
        .then(gotDevices)
        .then(infos => {
            
            return infos;
        })
        .catch((e) => {
            console.log('Error:', e.name, 'Message:', e.message);
            return [];
        });
}

export function getDeviceInfos (): Promise<MediaDeviceInfo[]> {
    return navigator.mediaDevices
        .enumerateDevices()
        .then(gotDevices)
        .catch((e) => {
            console.log(e);
            return [];
        });
}