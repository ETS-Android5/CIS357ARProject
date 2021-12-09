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
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import com.gorisse.thomas.sceneform.scene.await

class MainFragment : Fragment(R.layout.fragment_main) {

    private lateinit var arFragment: ArFragment
    private val arSceneView get() = arFragment.arSceneView
    private val scene get() = arSceneView.scene

    private var model1: Renderable? = null
    private var model2: Renderable? = null
    private var modelInUse: Renderable? = null
    private lateinit var modelView: ViewRenderable

    val anchors = mutableListOf<Anchor>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var newModel = view.findViewById<Button>(R.id.changeModel) as Button
        var animateModel = view.findViewById<Button>(R.id.animateModel) as Button

        arFragment = (childFragmentManager.findFragmentById(R.id.arFragment) as ArFragment).apply {
            setOnSessionConfigurationListener { session, config ->
                // Modify the AR session configuration here
            }
            setOnViewCreatedListener { arSceneView ->
                arSceneView.setFrameRateFactor(SceneView.FrameRate.FULL)

               newModel.setOnClickListener() { v: View? ->
                    switchView()
                }
            }
            setOnTapArPlaneListener(::onTapPlane)
        }

        lifecycleScope.launchWhenCreated {
            loadModels()
        }
    }

    private fun switchView() {
        if(modelInUse == model1){
            modelInUse = model2
        } else {
            modelInUse = model1
        }
    }

    private suspend fun loadModels() {
        model1 = ModelRenderable.builder()
            .setSource(context, Uri.parse("https://storage.googleapis.com/ar-answers-in-search-models/static/Tiger/model.glb"))
            .setIsFilamentGltf(true)
            .await()

        model2 = ModelRenderable.builder()
            .setSource(context, Uri.parse("models/halloween.glb"))
            .setIsFilamentGltf(true)
            .await()

        modelInUse = model1

        modelView = ViewRenderable.builder()
            .setView(context, R.layout.view_renderable_infos)
            .await()
    }


    

    private fun onTapPlane(hitResult: HitResult, plane: Plane, motionEvent: MotionEvent) {
        if (modelInUse == null || modelView == null) {
            Toast.makeText(context, "Loading...", Toast.LENGTH_SHORT).show()
            return
        }

        // Create the Anchor.
        val anchor1 = hitResult.createAnchor()

        scene.addChild(AnchorNode(anchor1).apply {
            // Create the transformable model and add it to the anchor.

            anchors.add(anchor1)

            if (anchors.size > 1) {
                anchors[0].detach()
                anchors.removeAt(0)
            }

            addChild(TransformableNode(arFragment.transformationSystem).apply {
                renderable = modelInUse
                //renderableInstance.animate(true).start()

                // Add the View
                addChild(Node().apply {
                    // Define the relative position
                    localPosition = Vector3(0f, 0f, -1.0f)
                    localScale = Vector3(0.3f, 0.3f, 0.3f)
                    renderable = modelView
                })
            })
        })
    }
}