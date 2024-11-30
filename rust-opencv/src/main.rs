use opencv::{ self as cv, highgui, prelude::*, videoio};
use mediapipe_rs::tasks::vision::FaceLandmarkerBuilder;
use mediapipe_rs::postprocess::utils::DrawLandmarksOptions;
use mediapipe_rs::tasks::vision::FaceLandmarkConnections;
use image::{DynamicImage, GenericImageView, ImageFormat, RgbImage};
//use std::error::Error;
use anyhow::{Result,  anyhow};
use ndarray::{Array1, ArrayView1, ArrayView3};




const MODEL_PATH: &'static str= "data/face_landmarker.task";

trait AsArray {
    fn try_as_array(&self) -> Result<ArrayView3<u8>>;
}impl AsArray for cv::core::Mat {
    fn try_as_array(&self) -> Result<ArrayView3<u8>> {
        if !self.is_continuous() {
            return Err(anyhow!("Mat is not continuous"));
        }
        let bytes = self.data_bytes()?;
        let size = self.size()?;
        let a = ArrayView3::from_shape((size.height as usize, size.width as usize, 3), bytes)?;
        Ok(a)
    }
}

fn array_to_image(arr: ArrayView3<u8>) -> RgbImage {
    assert!(arr.is_standard_layout());
let (height, width, _) = arr.dim();
    let raw = arr.to_slice().expect("Failed to extract slice from array");
RgbImage::from_raw(width as u32, height as u32, raw.to_vec())
        .expect("container should have the right size for the image dimensions")
}

fn main() -> Result<(), Box<dyn std::error::Error>>{
	let window = "video capture";
	highgui::named_window(window, highgui::WINDOW_AUTOSIZE)?;
	let mut cam = videoio::VideoCapture::new(0, videoio::CAP_ANY)?; // 0 is the default camera

	//let face_landmarker = FaceLandmarkerBuilder::new()
    //    .num_faces(1) // set max number of faces to detect
    //    .min_face_detection_confidence(0.5)
    //    .min_face_presence_confidence(0.5)
    //    .min_tracking_confidence(0.5)
    //    .output_face_blendshapes(true)
    //    .build_from_file(model_path); // create a face landmarker

	let opened = videoio::VideoCapture::is_opened(&cam)?;
	if !opened {
		panic!("Unable to open default camera!");
	}
	loop {
		
		let mut frame = Mat::default();

		
		cam.read(&mut frame)?;
		if frame.size()?.width > 0 {
            

            let a = frame.try_as_array()?;
            let img = array_to_image(a);

            println!("Here");
            let face_landmark_results = FaceLandmarkerBuilder::new()
                .num_faces(1) // set max number of faces to detect
                .min_face_detection_confidence(0.5)
                .min_face_presence_confidence(0.5)
                .min_tracking_confidence(0.5)
                .output_face_blendshapes(true)
                .build_from_file(MODEL_PATH)? // create a face landmarker
                .detect(&img)?;
            println!("There!");

			highgui::imshow(window, &frame)?;
			
			//let landmark_results = face_landmarker?.detect(&frame);
		}
		let key = highgui::wait_key(10)?;
		if key > 0 && key != 255 {
			break;
		}
	}
	Ok(())
}
