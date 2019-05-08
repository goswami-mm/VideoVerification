# VideoVerification

# Main screen has two four option - 
    1. Record video : It record video and take 5 Snapshots, user have to record min. 5 sec video recording. 
                      All snapsshots will return back to main screen for futher processing.
    2. Capture Pic : It has use tensorflow Lib, MTCNN to detect faces. Texture view inflate live view with rect box tracker                        around user's face. Cropped Image will return back to main screen for futher processing.  
    3. Record Audio : It generate random 5 digit number, there option record users audio. It will use Google speach to text                        lib to match genrated number. The result will return back to main screen. 
    4. Verify video : This will process all snapshots with user pic. Facenet and SVC used to validate result. 

# This project use NDK feature for processing Face Detection and tracking with help of Tensorflow.
# Used Lib :
  1. Tensorflow 
  2. FaceNet
  3. SVC
  
# Link :
https://github.com/goswami-mm/VideoVerification/

  
                      
