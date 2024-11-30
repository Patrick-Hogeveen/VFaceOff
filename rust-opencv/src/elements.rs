use opencv::{core as core_cv, prelude::*};

use half::f16;

pub use element_type::*;
pub mod element_type {
    use super::*;

    pub trait OpenCvElement {
        const DEPTH: i32;
    }

    impl OpenCvElement for u8 {
        const DEPTH: i32 = core_cv::CV_8U;
    }

    impl OpenCvElement for i8 {
        const DEPTH: i32 = core_cv::CV_8S;
    }

    impl OpenCvElement for u16 {
        const DEPTH: i32 = core_cv::CV_16U;
    }

    impl OpenCvElement for i16 {
        const DEPTH: i32 = core_cv::CV_16S;
    }

    impl OpenCvElement for i32 {
        const DEPTH: i32 = core_cv::CV_32S;
    }

    impl OpenCvElement for f16 {
        const DEPTH: i32 = core_cv::CV_16F;
    }

    impl OpenCvElement for f32 {
        const DEPTH: i32 = core_cv::CV_32F;
    }

    impl OpenCvElement for f64 {
        const DEPTH: i32 = core_cv::CV_64F;
    }
}