package com.google.ar.sceneform.samples.gltf

import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.Toast

import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.ar.core.Anchor
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.SceneView
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.rendering.RenderableInstance
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import com.gorisse.thomas.sceneform.scene.await

/*
    The Following is our modifications to the "sample-ar-model-viewer" sample from the forked github
    repository demonstrating ARCore and Sceneform. We have studied the functionality of the code
    provided and modified it to understand how to limit placement of models, change the model through
    input, and animate models based on user input. (Useful for our original "My AR Buddy" idea).

    Sarah Rockow (2022) & Wyatt Davis (2023)
    Fall 2023
 */
class MainFragment : Fragment(R.layout.fragment_main) {

    private lateinit var arFragment: ArFragment
    private val arSceneView get() = arFragment.arSceneView
    private val scene get() = arSceneView.scene

    /* The following three variables were added by us, as part of our solution to allow user's to
       change the model of the placeable object manually. */

    // This is a Renderable object that will hold our first model
    private var model1: Renderable? = null

    // This is a Renderable object that will hold our second model
    private var model2: Renderable? = null

    // This is a Renderable object that will hold the model we want to use
    private var modelInUse: Renderable? = null
    private lateinit var modelView: ViewRenderable

    /* The following variable was added by us, so that the placed model would have a reference
       and could be animated after being placed. */

    // This is a RenderableInstance object that will hold the instance of the object to be placed
    private var renderedInstance: RenderableInstance? = null

    /* The following variable was added by us, so that the application could track the number of
       placed objects, and restrict it to only 2. */

    // This a mutable list of Anchor objects
    val anchors = mutableListOf<Anchor>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // This variable holds a reference to our button that allows the user to use a different model
        var newModel = view.findViewById<Button>(R.id.changeModel) as Button

        // This variable holds a reference to our button that allows the user to animate the current model
        var animateModel = view.findViewById<Button>(R.id.animateModel) as Button

        // Calls the switchView function when the newModel button is pressed
        newModel.setOnClickListener() { v: View? ->
            switchView()
        }

        // Calls the animatedMethod function when the animateModel button is pressed
        animateModel.setOnClickListener() { v: View? ->
            animateMethod()
        }

        arFragment = (childFragmentManager.findFragmentById(R.id.arFragment) as ArFragment).apply {
            setOnSessionConfigurationListener { session, config ->
            }

            setOnViewCreatedListener { arSceneView ->
                arSceneView.setFrameRateFactor(SceneView.FrameRate.FULL) }
            setOnTapArPlaneListener(::onTapPlane)
        }

        lifecycleScope.launchWhenCreated {
            loadModels()
        }
    }

    /* This function will switch the model that is currently being placed */
    private fun switchView() {

        // If the current model is model1...
        if(modelInUse == model1)

            // Switch it to model2
            modelInUse = model2

        // Otherwise...
        else

            // Switch it to model1
            modelInUse = model1
    }

    /* Our modification of the original loadModels method loads multiple models in for use */
    private suspend fun loadModels() {

        // Model1 is define as a model of a tiger pulled from a web source
        model1 = ModelRenderable.builder()
            .setSource(context, Uri.parse("https://storage.googleapis.com/ar-answers-in-search-models/static/Tiger/model.glb"))
            .setIsFilamentGltf(true)
            .await()

        // Model2 is define as a model pulled from the sample assets
        model2 = ModelRenderable.builder()
            .setSource(context, Uri.parse("models/halloween.glb"))
            .setIsFilamentGltf(true)
            .await()

        // The default model to use is set to model1
        modelInUse = model1

        // The model view stands by to build a model
        modelView = ViewRenderable.builder()
            .setView(context, R.layout.view_renderable_infos)
            .await()
    }

    /* Our modification of the original onTapPlane function restricts the amount of models you can
       place, and does not initially animate the model (this is saved for the animate function). */
    private fun onTapPlane(hitResult: HitResult, plane: Plane, motionEvent: MotionEvent) {

        // Safeguard against trying to place models before they've been loaded
        if (modelInUse == null || modelView == null) {
            Toast.makeText(context, "Loading...", Toast.LENGTH_SHORT).show()
            return
        }

        // Create the anchor for the model to be placed
        val anchor1 = hitResult.createAnchor()

        // Add this to the scene
        scene.addChild(AnchorNode(anchor1).apply {

            // Add the anchor to out list of anchors (assists us in tracking the amount of models)
            anchors.add(anchor1)

            // If the anchor size is greater than 2...
            if (anchors.size > 2) {

                // Detach the first anchor in the list
                anchors[0].detach()

                // Remove that anchor from the list
                anchors.removeAt(0)
            }

            // Add the model
            addChild(TransformableNode(arFragment.transformationSystem).apply {

                // Store the current model being used
                renderable = modelInUse

                // Store the rendered instance into our state variable
                renderedInstance = renderableInstance


                // Add the view
                addChild(Node().apply {

                    // Place the model at these positions
                    localPosition = Vector3(0f, 0f, -1.0f)
                    localScale = Vector3(0.3f, 0.3f, 0.3f)
                    renderable = modelView
                })
            })
        })
    }

    /* This is out function that allows the user to animate the models through input */
    private fun animateMethod() {

        // Animates the Renderable Instance in out renderedInstance state variable
        renderedInstance?.animate(true)?.start()
    }
}