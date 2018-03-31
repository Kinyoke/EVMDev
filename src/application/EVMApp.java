package application;
	

import java.io.FileInputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;
import org.opencv.videoio.VideoCapture;

import application.data.dataHandler;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;


/*****************************************************
 *	@author : Faisal burhan Abdu.                    *
 *	@version : v1.0.1.            					 *
 *	@date : 2017-01-14. 							 *
 ****************************************************/

public class EVMApp extends Application {
	
	private static int position = 0;
	private static int clicks = 0;
	private static int vVal = 0;
	TranslateTransition translateTransition;
	
	private Button acqFace;
	private ImageView imageView2;
	private CheckBox haarClassifier;
	private CheckBox lbpClassifier;
	private VBox mainPane;
	// a timer for acquiring the video stream
	private ScheduledExecutorService timer;
	// the OpenCV object that performs the video capture
	private VideoCapture capture;
	// a flag to change the button behavior
	private boolean cameraActive;
	// face cascade classifier
	private CascadeClassifier faceCascade;
	private int absoluteFaceSize;
	private Text myFullName;
	private Text myBirthDay;
	private Text myGender;
	private Text myResidence;
	
	/**
	 * Get a frame from the opened video stream (if any)
	 * 
	 * @return the {@link Image} to show
	 */
	private Mat grabFrame()
	{
		Mat frame = new Mat();
		
		// check if the capture is open
		if (this.capture.isOpened())
		{
			try
			{
				// read the current frame
				this.capture.read(frame);
				
				// if the frame is not empty, process it
				if (!frame.empty())
				{
					// face detection
					this.detectAndDisplay(frame);
				}
				
			}
			catch (Exception e)
			{
				// log the (full) error
				System.err.println("Exception during the image elaboration: " + e);
			}
		}
		
		return frame;
	}
	
	/**
	 * Method for face detection and tracking
	 * 
	 * @param frame
	 *            it looks for faces in this frame
	 */
	private void detectAndDisplay(Mat frame)
	{
		MatOfRect faces = new MatOfRect();
		Mat grayFrame = new Mat();
		
		// convert the frame in gray scale
		Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);
		// equalize the frame histogram to improve the result
		Imgproc.equalizeHist(grayFrame, grayFrame);
		
		// compute minimum face size (20% of the frame height, in our case)
		if (this.absoluteFaceSize == 0)
		{
			int height = grayFrame.rows();
			if (Math.round(height * 0.2f) > 0)
			{
				this.absoluteFaceSize = Math.round(height * 0.2f);
			}
		}
		
		// detect faces
		this.faceCascade.detectMultiScale(grayFrame, faces, 1.1, 2, 0 | Objdetect.CASCADE_SCALE_IMAGE,
				new Size(this.absoluteFaceSize, this.absoluteFaceSize), new Size());
				
		// each rectangle in faces is a face: draw them!
		Rect[] facesArray = faces.toArray();
		for (int i = 0; i < facesArray.length; i++)
			Imgproc.rectangle(frame, facesArray[i].tl(), facesArray[i].br(), new Scalar(0, 255, 0), 3);
			
	}
	
	
	
	/**
	 * Method for loading a classifier trained set from disk
	 * 
	 * @param classifierPath
	 *            the path on disk where a classifier trained set is located
	 */
	private void checkboxSelection(String classifierPath)
	{
		// load the classifier(s)
		this.faceCascade.load(classifierPath);
		
		// now the video capture can start
		this.acqFace.setDisable(false);
	}
	
	/**
	 * Stop the acquisition from the camera and release all the resources
	 */
	private void stopAcquisition()
	{
		if (this.timer!=null && !this.timer.isShutdown())
		{
			try
			{
				// stop the timer
				this.timer.shutdown();
				this.timer.awaitTermination(33, TimeUnit.MILLISECONDS);
			}
			catch (InterruptedException e)
			{
				// log any exception
				System.err.println("Exception in stopping the frame capture, trying to release the camera now... " + e);
			}
		}
		
		if (this.capture.isOpened())
		{
			// release the camera
			this.capture.release();
		}
	}
	
	/**
	 * Update the {@link ImageView} in the JavaFX main thread
	 * 
	 * @param view
	 *            the {@link ImageView} to update
	 * @param image
	 *            the {@link Image} to show
	 */
	private void updateImageView(ImageView view, Image image)
	{
		Utils.onFXThread(view.imageProperty(), image);
	}

	
	@SuppressWarnings("static-access")
	@Override
	public void start(Stage primaryStage) {
		try {
			
			this.capture = new VideoCapture();
			this.faceCascade = new CascadeClassifier();
			this.absoluteFaceSize = 0;
			
			
			haarClassifier = new CheckBox("Haar Classifier");
			lbpClassifier = new CheckBox("LBP Classifier");
			if(haarClassifier.isSelected())lbpClassifier.setSelected(false);
			if(lbpClassifier.isSelected())haarClassifier.setSelected(false);
			
			Rectangle pane = new Rectangle();
			pane.setX(0);
			pane.setY(0);
			pane.setHeight(70);
			pane.setWidth(320);
			pane.setFill(Color.color(0.1, 0.1, 0.1));
			
			Rectangle bholder = new Rectangle();
			bholder.setY(0);
			bholder.setX(0);
			bholder.setWidth(70);
			bholder.setHeight(45);
			bholder.setArcHeight(20);
			bholder.setArcWidth(20);
			bholder.setFill(Color.color(0.2, 0.2, 0.2));
			
			Rectangle angtp = new Rectangle();
			angtp.setY(0);
			angtp.setX(0);
			angtp.setHeight(5);
			angtp.setWidth(20);
			angtp.setArcHeight(5);
			angtp.setArcWidth(5);
			angtp.setFill(Color.color(0.1, 0.1, 0.1));
			angtp.setRotate(45);
			
			Rectangle angbt = new Rectangle();
			angbt.setY(0);
			angbt.setX(0);
			angbt.setHeight(5);
			angbt.setWidth(20);
			angbt.setArcHeight(5);
			angbt.setArcWidth(5);
			angbt.setFill(Color.color(0.1, 0.1, 0.1));
			angbt.setRotate(135);
			
			StackPane stackPane2 = new StackPane();
			//Setting the margin for the circle
			stackPane2.setPrefSize(50, 45);
			stackPane2.setLayoutX(125);
			stackPane2.setLayoutY(548);
			stackPane2.setMargin(bholder, new Insets(0, 0, 0, 0));
			stackPane2.setMargin(angtp, new Insets(-5, 15, 3, 0));
			stackPane2.setMargin(angbt, new Insets(-5, 0, 3, 10));
			//Retrieving the observable list of the Stack Pane
			ObservableList<Node> list4 = stackPane2.getChildren();
			//Adding all the nodes to the pane
			list4.addAll(bholder, angtp, angbt);
			
			//creating a text field
			Text header = new Text("Registration Form");
			header.setFill(Color.GRAY);
			header.setFont(Font.font("cursive", FontWeight.BOLD, 20));

			StackPane stackPane = new StackPane();
			stackPane.setPrefHeight(70);
			stackPane.setMargin(pane, new Insets(0, 250, 0, 0));
			stackPane.setMargin(header, new Insets(-2, 250, 0, 0));
			//Retrieving the observable list of the Stack Pane
			ObservableList<Node> list3 = stackPane.getChildren();
			//Adding all the nodes to the pane
			list3.addAll(pane,header);
			
			TextField fname = new TextField();
			fname.setText("		First Name");
			fname.setPrefHeight(30);
			fname.setStyle("-fx-background-color: rgb(40,40,40); -fx-text-fill: gray;");
			TextField mname = new TextField();
			mname.setText("		Middle Name");
			mname.setPrefHeight(30);
			mname.setStyle("-fx-background-color: rgb(40,40,40); -fx-text-fill: gray;");
			TextField lname = new TextField();
			lname.setText("		Last Name");
			lname.setPrefHeight(30);
			lname.setStyle("-fx-background-color: rgb(40,40,40); -fx-text-fill: gray;");
			TextField birthDate = new TextField();
			birthDate.setText("		yyyy - mm - dd  ");
			birthDate.setPrefHeight(30);
			birthDate.setStyle("-fx-background-color: rgb(40,40,40); -fx-text-fill: gray;");
			Text gender = new Text("Gender:");
			gender.setFill(Color.GRAY);
			Text residence = new Text("Residence:");
			residence.setFill(Color.GRAY);
			//Toggle group of radio buttons
			ToggleGroup groupGender = new ToggleGroup();
			RadioButton maleRadio = new RadioButton("Male");
			maleRadio.setToggleGroup(groupGender);
			maleRadio.setTextFill(Color.GRAY);
			RadioButton femaleRadio = new RadioButton("Female");
			femaleRadio.setToggleGroup(groupGender);
			femaleRadio.setTextFill(Color.GRAY);
			femaleRadio.setTranslateX(90);;
			
			ChoiceBox<String> region = new ChoiceBox<String>();
			region.getItems().addAll("Dar-es-salaam", "Mwanza", "Dodoma",
			"Arusha", "Tabora");
			region.setPrefWidth(300);
			region.setStyle("-fx-background-color: rgb(40,40,40); -fx-text-fill: white;");
			ChoiceBox<String> district = new ChoiceBox<String>();
			district.getItems().addAll("Magomeni", "Kinondoni", "Temeke");
			district.setPrefWidth(300);
			district.setStyle("-fx-background-color: rgb(40,40,40); -fx-text-fill: gray;");
			ChoiceBox<String> ward = new ChoiceBox<String>();
			ward.getItems().addAll("Buguruni", "Kimara", "Mbezi",
			"Masaki", "Mbezi beach");
			ward.setPrefWidth(300);
			ward.setStyle("-fx-background-color: rgb(40,40,40); -fx-text-fill: gray;");
			ChoiceBox<String> street = new ChoiceBox<String>();
			street.getItems().addAll("Marapa", "Mikumi", "Rozana",
			"Karume", "Mapipa");
			street.setPrefWidth(300);
			street.setStyle("-fx-background-color: rgb(40,40,40); -fx-text-fill: gray;");
			//Instantiating the HBox class
			HBox hBox = new HBox();
			hBox.setPrefWidth(340);
			hBox.setMargin(maleRadio, new Insets(5, 25, 5, 25));
			hBox.setMargin(femaleRadio, new Insets(5, 0, 5, 25));
			ObservableList<Node> list2  = hBox.getChildren();
			list2.addAll(maleRadio,femaleRadio);
			
			
			//finger print widgets
			Image image = new Image(new FileInputStream("/opt/lampp/htdocs/EVM/images/fingerprint.png"));
			//Setting the image view
			ImageView imageView = new ImageView(image);
			//Setting the position of the image
			imageView.setX(0);
			imageView.setY(0);
			//setting the fit height and width of the image view
			imageView.setFitHeight(200);
			imageView.setFitWidth(200);
			//Setting the preserve ratio of the image view
			imageView.setPreserveRatio(true);
		
			Rectangle imgFrame = new Rectangle();
			imgFrame.setX(0);
			imgFrame.setY(0);
			imgFrame.setHeight(250);
			imgFrame.setWidth(240);
			imgFrame.setArcHeight(10);
			imgFrame.setArcWidth(10);
			imgFrame.setFill(Color.color(0.2, 0.2, 0.2));
			imgFrame.setStrokeType(StrokeType.OUTSIDE);
			imgFrame.setStrokeWidth(2);
			imgFrame.setStroke(Color.color(0.3, 0.3, 0.3));
			
			Rectangle hscale = new Rectangle();
			hscale.setY(0);
			hscale.setX(0);
			hscale.setWidth(240);
			hscale.setHeight(5);
			hscale.setFill(Color.TRANSPARENT);;
			
			Rectangle vScale1 = new Rectangle();
			vScale1.setY(0);
			vScale1.setX(0);
			vScale1.setWidth(10);
			vScale1.setHeight(50);
			vScale1.setArcHeight(20);
			vScale1.setArcWidth(20);
			vScale1.setFill(Color.color(0.1, 0.1, 0.1));
			
			Rectangle vScale2 = new Rectangle();
			vScale2.setY(0);
			vScale2.setX(0);
			vScale2.setWidth(10);
			vScale2.setHeight(50);
			vScale2.setArcHeight(20);
			vScale2.setArcWidth(20);
			vScale2.setFill(Color.color(0.1, 0.1, 0.1));
			StackPane scale = new StackPane();
			scale.setTranslateX(0);
			scale.setTranslateY(0);
			scale.setLayoutY(0);
			scale.setLayoutX(0);
			scale.setMargin(hscale, new Insets(0, 0, 0, 0));
			scale.setMargin(vScale1, new Insets(0, 0, 0, -245));
			scale.setMargin(vScale2, new Insets(0, 0, 0, 245));
			//Retrieving the observable list of the Stack Pane
			ObservableList<Node> listS = scale.getChildren();
			//Adding all the nodes to the pane
			listS.addAll(hscale, vScale1, vScale2);
			
			StackPane stackPane3 = new StackPane();
			stackPane3.setPrefHeight(70);
			//Setting the margin for the circle
			stackPane3.setMargin(imgFrame, new Insets(0, 0, 0, 0));
			stackPane3.setMargin(imageView, new Insets(0, 0, 0, 0));
			stackPane3.setMargin(scale, new Insets(220, 0, 0, 0));
			//Retrieving the observable list of the Stack Pane
			ObservableList<Node> list6 = stackPane3.getChildren();
			//Adding all the nodes to the pane
			list6.addAll(imgFrame, imageView, scale);
			
			myFullName = new Text("Fist middle last");
			myFullName.setFill(Color.GRAY);
			myBirthDay = new Text("2018-10-10");
			myBirthDay.setFill(Color.GRAY);
			myGender = new Text("Male");
			myGender.setFill(Color.GRAY);
			myResidence = new Text("Dar-es-salaam, Temeke, Buguruni, \nMalapa");
			myResidence.setFill(Color.GRAY);
			
			Button acqFinger = new Button("scan fingerprint");
			acqFinger.setTranslateX(35);
			acqFinger.setTextFill(Color.WHITE);
			acqFinger.setPrefSize(250, 35);
			acqFinger.setStyle("-fx-background-color: rgb(50,50,50)");
			acqFinger.setCursor(Cursor.HAND);
								
			//facial scan widgets
			//Setting the image view
			imageView2 = new ImageView();
			//Setting the position of the image
			imageView2.setX(0);
			imageView2.setY(0);
			//setting the fit height and width of the image view
			imageView2.setFitHeight(250);
			imageView2.setFitWidth(240);
			//Setting the preserve ratio of the image view
			imageView2.setPreserveRatio(true);
		
			Rectangle imgFrame2 = new Rectangle();
			imgFrame2.setX(0);
			imgFrame2.setY(0);
			imgFrame2.setHeight(250);
			imgFrame2.setWidth(240);
			imgFrame2.setArcHeight(10);
			imgFrame2.setArcWidth(10);
			imgFrame2.setFill(Color.color(0.2, 0.2, 0.2));
			imgFrame2.setStrokeType(StrokeType.OUTSIDE);
			imgFrame2.setStrokeWidth(2);
			imgFrame2.setStroke(Color.color(0.3, 0.3, 0.3));
			
			StackPane stackPane4 = new StackPane();
			stackPane4.setPrefHeight(70);
			//Setting the margin for the circle
			stackPane4.setMargin(imgFrame2, new Insets(0, 0, 0, 0));
			stackPane4.setMargin(imageView2, new Insets(0, 0, 0, 0));
			//Retrieving the observable list of the Stack Pane
			ObservableList<Node> list8 = stackPane4.getChildren();
			//Adding all the nodes to the pane
			list8.addAll(imgFrame2, imageView2);
			
			
			Text myFullName2 = new Text("Fist middle last");
			myFullName2.setFill(Color.GRAY);
			Text myBirthDay2 = new Text("2018-10-10");
			myBirthDay2.setFill(Color.GRAY);
			Text myGender2 = new Text("Male");
			myGender2.setFill(Color.GRAY);
			Text myResidence2 = new Text("Dar-es-salaam, Temeke, Buguruni, \nMalapa");
			myResidence2.setFill(Color.GRAY);
			
			haarClassifier = new CheckBox();
			haarClassifier.setSelected(true);
			lbpClassifier = new CheckBox();
			
			acqFace = new Button("run facial recognition");
			acqFace.setTranslateX(35);
			acqFace.setTextFill(Color.WHITE);
			acqFace.setPrefSize(250, 35);
			acqFace.setStyle("-fx-background-color: rgb(50,50,50)");
			acqFace.setCursor(Cursor.HAND);
		
			VBox vBox = new VBox();
			//Setting the space between the nodes of a HBox pane
			vBox.setSpacing(2);
			//Setting the margin to the nodes
			vBox.setMargin(fname, new Insets(30, 25, 10, 25)); //top margin, right margin, bottom margin, left margin 
			vBox.setMargin(mname, new Insets(10, 25, 5, 25));
			vBox.setMargin(lname, new Insets(10, 25, 5, 25));
			vBox.setMargin(birthDate, new Insets(10, 25, 5, 25));
			vBox.setMargin(gender, new Insets(20, 25, 0, 25));
			vBox.setMargin(hBox, new Insets(10, 25, 5, 0));
			vBox.setMargin(residence, new Insets(20, 25, 5, 25));
			vBox.setMargin(region, new Insets(10, 25, 5, 25));
			vBox.setMargin(district, new Insets(5, 25, 5, 25));
			vBox.setMargin(ward, new Insets(5, 25, 5, 25));
			vBox.setMargin(street, new Insets(5, 25, 5, 25));
			//retrieving the observable list of the HBox
			ObservableList<Node> list = vBox.getChildren();
			//Adding all the nodes to the observable list
			list.addAll(fname,mname,lname, birthDate, gender, hBox, residence, region, district, ward, street);
			
			
			VBox vBox2 = new VBox();
			//Setting the space between the nodes of a HBox pane
			vBox2.setSpacing(2);
			//Setting the margin to the nodes
			vBox2.setMargin(stackPane3, new Insets(30, 25, 10, 25)); //top margin, right margin, bottom margin, left margin 
			vBox2.setMargin(myFullName, new Insets(30, 25, 5, 40));
			vBox2.setMargin(myBirthDay, new Insets(0, 25, 5, 40));
			vBox2.setMargin(myGender, new Insets(0, 25, 5, 40));
			vBox2.setMargin(myResidence, new Insets(0, 25, 5, 40));
			vBox2.setMargin(acqFinger, new Insets(9, 25, 5, 0));
			//retrieving the observable list of the HBox
			ObservableList<Node> list5 = vBox2.getChildren();
			//Adding all the nodes to the observable list
			list5.addAll(stackPane3, myFullName, myBirthDay, myGender, myResidence, acqFinger);
			
			VBox vBox3 = new VBox();
			//Setting the space between the nodes of a HBox pane
			vBox3.setSpacing(2);
			//Setting the margin to the nodes
			vBox3.setMargin(stackPane4, new Insets(30, 25, 10, 25)); //top margin, right margin, bottom margin, left margin 
			vBox3.setMargin(myFullName2, new Insets(30, 25, 5, 40));
			vBox3.setMargin(myBirthDay2, new Insets(0, 25, 5, 40));
			vBox3.setMargin(myGender2, new Insets(0, 25, 5, 40));
			vBox3.setMargin(myResidence2, new Insets(0, 25, 20, 40));
			vBox3.setMargin(acqFace, new Insets(10, 25, 5, 0));
			//retrieving the observable list of the HBox
			ObservableList<Node> list7 = vBox3.getChildren();
			//Adding all the nodes to the observable list
			list7.addAll(stackPane4, myFullName2, myBirthDay2, myGender2, myResidence2, acqFace);
			
			
			BorderPane root = new BorderPane(vBox);
			root.setPrefSize(320, 550);
			root.setLayoutY(60);
			
			BorderPane root2 = new BorderPane(vBox2);
			root2.setPrefSize(320, 550);
			root2.setLayoutY(60);
			
			BorderPane root3 = new BorderPane(vBox3);
			root3.setPrefSize(320, 550);
			root3.setLayoutY(60);
			
			
			bholder.setOnMouseEntered(new EventHandler<MouseEvent>(){

				@Override
				public void handle(MouseEvent event) {
					// TODO Auto-generated method stub
					bholder.setFill(Color.BLACK);
					angtp.setFill(Color.color(0.2, 0.2, 0.2));
					angbt.setFill(Color.color(0.2, 0.2, 0.2));
	                bholder.setCursor(Cursor.HAND);
					
				}
				
			});
			
			bholder.setOnMouseExited(new EventHandler<MouseEvent>(){

				@Override
				public void handle(MouseEvent event) {
					// TODO Auto-generated method stub
					bholder.setFill(Color.color(0.2, 0.2, 0.2));
					angtp.setFill(Color.color(0.1, 0.1, 0.1));
					angbt.setFill(Color.color(0.1, 0.1, 0.1));
				}
				
			});
			
		acqFinger.setOnMouseClicked(new EventHandler<MouseEvent>(){

			@Override
			public void handle(MouseEvent event) {
				// TODO Auto-generated method stub
				vVal+=1;
				if(vVal == 1){
					//Creating Translate Transition
					translateTransition = new TranslateTransition();
					//Setting the duration of the transition
					translateTransition.setDuration(Duration.millis(2000));
					hscale.setFill(Color.color(0.1, 0.8, 0.1, 0.9));
					imgFrame.setFill(Color.color(0.2, 0.5, 0.2,0.2));
					translateTransition.setNode(scale);
					if(scale.getLayoutY() > 100) translateTransition.setByY(-210);
					//Setting the cycle count for the transition
					translateTransition.setCycleCount(2);
					//Setting auto reverse value to false
					translateTransition.setAutoReverse(false);
					//Playing the animation
					translateTransition.play();
					acqFinger.setText("Verify finger print");
				}
				if(vVal == 2){
					//Creating Translate Transition
					translateTransition = new TranslateTransition();
					//Setting the duration of the transition
					translateTransition.setDuration(Duration.millis(2000));
					hscale.setFill(Color.color(0.1, 0.8, 0.1, 0.9));
					imgFrame.setFill(Color.color(0.2, 0.5, 0.2,0.2));
					translateTransition.setNode(scale);
					if(scale.getLayoutY() > 100) translateTransition.setByY(210);
					//Setting the cycle count for the transition
					translateTransition.setCycleCount(2);
					//Setting auto reverse value to false
					translateTransition.setAutoReverse(false);
					//Playing the animation
					translateTransition.play();
					acqFinger.setDisable(true);
				}
			}
			
		});
		
		acqFace.setOnMouseClicked(new EventHandler<MouseEvent>(){

			@Override
			public void handle(MouseEvent event) {
				// TODO Auto-generated method stub
				if (!cameraActive)
				{
					// disable setting checkboxes
					haarClassifier.setSelected(false);
					haarClassifier.setSelected(true);
					checkboxSelection("resources/haarcascades/haarcascade_frontalface_alt.xml");
					lbpClassifier.setDisable(true);
					
					// start the video capture
					capture.open(0);
					
					// is the video stream available?
					if (capture.isOpened())
					{
						cameraActive = true;
						
						// grab a frame every 33 ms (30 frames/sec)
						Runnable frameGrabber = new Runnable() {
							
							@Override
							public void run()
							{
								// effectively grab and process a single frame
								Mat frame = grabFrame();
								// convert and show the frame
								Image imageToShow = Utils.mat2Image(frame);
								updateImageView(imageView2, imageToShow);
							}
						};
						
						timer = Executors.newSingleThreadScheduledExecutor();
						timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);
						
//						OpenCVFrameGrabber grabImg = OpenCVFrameGrabber(0);
						
					}
					else
					{
						// log the error
						System.err.println("Failed to open the camera connection...");
					}
					acqFace.setText("submit data");
				}else{
					// the camera is not active at this point
					cameraActive = false;
					// stop the timer
					stopAcquisition();
					acqFace.setDisable(true);
					//refresh form 
					translateTransition = new TranslateTransition();
					//Setting the duration of the transition
					translateTransition.setDuration(Duration.millis(1000));
					//Setting auto reverse value to false
					translateTransition.setAutoReverse(false);
					translateTransition.setNode(mainPane);
					translateTransition.setFromY(0);
					translateTransition.setToY(0);
					translateTransition.play();
					//send data to the db for storage
					String myGender = "";
					if(groupGender.getSelectedToggle().equals(maleRadio)) myGender = maleRadio.getText();
					if(groupGender.getSelectedToggle().equals(femaleRadio)) myGender = femaleRadio.getText();
					String myResidence = region.getValue()+","+district.getValue()+","+ward.getValue()+","+street.getValue();
					String data = "'"+fname.getText()+"','"+mname.getText()+"','"+lname.getText()+"','"+birthDate.getText()+"','"+myGender+"','"+myResidence+"','',''";
					dataHandler.init();
					dataHandler.setData(data);
					dataHandler.exit();
					//refresh the form and initiate new user session.
					System.out.println(fname.getText());
					System.out.println(mname.getText());
					System.out.println(lname.getText());
					System.out.println(birthDate.getText());
					if(groupGender.getSelectedToggle().equals(maleRadio))System.out.println(maleRadio.getText());
					if(groupGender.getSelectedToggle().equals(femaleRadio))System.out.println(femaleRadio.getText());
					System.out.println(region.getValue());
					System.out.println(district.getValue());
					System.out.println(ward.getValue());
					System.out.println(street.getValue());
				}
			}
			
		});
			
			
			
			//here goes input pages | form feed | finger print | facial rec.
			mainPane = new VBox();
			mainPane.setPrefWidth(320);
			//mainPane.setPrefHeight(580);
			mainPane.setMargin(root, new Insets(40, 0, 0, 0));
			mainPane.setMargin(root2, new Insets(40, 0, 0, 0));
			mainPane.setMargin(root3, new Insets(70, 0, 0, 0));
			ObservableList<Node> listMain  = mainPane.getChildren();
			listMain.addAll(root, root2, root3);
			
			stackPane2.setOnMouseClicked(new EventHandler<MouseEvent>(){

				@Override
				public void handle(MouseEvent event) {
					// TODO Auto-generated method stub
					translateTransition = new TranslateTransition();
					//Setting the duration of the transition
					translateTransition.setDuration(Duration.millis(1000));
					//Setting auto reverse value to false
					translateTransition.setAutoReverse(false);
					translateTransition.setNode(mainPane);
					clicks+=1;
					if(position != -640) position+=-320;
					if(position == -320){ header.setText("Finger print"); header.setTranslateX(-10); }
					if(position == -640){ header.setText("Facial scan"); header.setTranslateX(-2); stackPane2.setVisible(false); }
					switch(clicks){
					case 1:
						myFullName.setText(fname.getText()+" "+mname.getText()+" "+lname.getText());
						myBirthDay.setText(birthDate.getText());
						String gender = "";
						if(groupGender.getSelectedToggle().equals(maleRadio)) gender = maleRadio.getText();
						if(groupGender.getSelectedToggle().equals(femaleRadio)) gender = femaleRadio.getText();
						myGender.setText(gender);;
						myResidence.setText(region.getValue()+", "+district.getValue()+", "+ward.getValue()+",\n"+street.getValue());
						translateTransition.setFromY(0);
						translateTransition.setToY(-590);
						translateTransition.play();
						break;

					case 2:
						myFullName2.setText(fname.getText()+" "+mname.getText()+" "+lname.getText());
						myBirthDay2.setText(birthDate.getText());
						String gender1 = "";
						if(groupGender.getSelectedToggle().equals(maleRadio)) gender1 = maleRadio.getText();
						if(groupGender.getSelectedToggle().equals(femaleRadio)) gender1 = femaleRadio.getText();
						myGender2.setText(gender1);;
						myResidence2.setText(region.getValue()+", "+district.getValue()+", "+ward.getValue()+",\n"+street.getValue());;
						translateTransition.setFromX(0);
						translateTransition.setFromY(-590);
						translateTransition.setToY(-1200);
						translateTransition.play();
						break;
				}
				}
				
			});
		
			Group group = new Group(mainPane, stackPane, stackPane2);
			root.setStyle("-fx-background-color: rgb(25,25,25)");
			root2.setStyle("-fx-background-color: rgb(25,25,25)");
			root3.setStyle("-fx-background-color: rgb(25,25,25)");
			Scene scene = new Scene(group, 320, 580, Color.color(0.1, 0.1, 0.1));
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			primaryStage.setTitle("EVM");
			primaryStage.setResizable(false);
			primaryStage.setScene(scene);
			primaryStage.setOnCloseRequest((new EventHandler<WindowEvent>() {
				public void handle(WindowEvent we)
				{
					stopAcquisition();
				}
			}));
			primaryStage.show();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	
	
	
	public static void main(String[] args) {
		// load the native OpenCV library
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		launch(args);
	}
}
