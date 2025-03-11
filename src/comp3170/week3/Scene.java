package comp3170.week3;

import static org.lwjgl.opengl.GL11.GL_FILL;
import static org.lwjgl.opengl.GL11.GL_FRONT_AND_BACK;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL11.glDrawElements;
import static org.lwjgl.opengl.GL11.glPolygonMode;
import static org.lwjgl.opengl.GL15.glBindBuffer;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import comp3170.GLBuffers;
import comp3170.Shader;
import comp3170.ShaderLibrary;

public class Scene {

	final private String VERTEX_SHADER = "vertex.glsl";
	final private String FRAGMENT_SHADER = "fragment.glsl";

	private Vector4f[] vertices;
	private int vertexBuffer;
	private int[] indices;
	private int indexBuffer;
	private Vector3f[] colours;
	private int colourBuffer;

	private Shader shader;
	
	private long oldTime;
	
	private static final float TRANSLATION_SPEED = 2.5f;
	private static final float ROTATION_SPEED = -5f;
	
	private float currentRotation = 0f;
	
	// define an empty destination Matrix
	private Matrix4f destMatrix = new Matrix4f();

	public Scene() {

		shader = ShaderLibrary.instance.compileShader(VERTEX_SHADER, FRAGMENT_SHADER);

		// @formatter:off
			//          (0,1)
			//           /|\
			//          / | \
			//         /  |  \
			//        / (0,0) \
			//       /   / \   \
			//      /  /     \  \
			//     / /         \ \		
			//    //             \\
			//(-1,-1)           (1,-1)
			//
	 		
		vertices = new Vector4f[] {
			new Vector4f( 0, 0, 0, 1),
			new Vector4f( 0, 1, 0, 1),
			new Vector4f(-1,-1, 0, 1),
			new Vector4f( 1,-1, 0, 1),
		};
			
			// @formatter:on
		vertexBuffer = GLBuffers.createBuffer(vertices);

		// @formatter:off
		colours = new Vector3f[] {
			new Vector3f(1,0,1),	// MAGENTA
			new Vector3f(1,0,1),	// MAGENTA
			new Vector3f(1,0,0),	// RED
			new Vector3f(0,0,1),	// BLUE
		};
			// @formatter:on

		colourBuffer = GLBuffers.createBuffer(colours);

		// @formatter:off
		indices = new int[] {  
			0, 1, 2, // left triangle
			0, 1, 3, // right triangle
			};
			// @formatter:on

		indexBuffer = GLBuffers.createIndexBuffer(indices);
		
		oldTime = System.currentTimeMillis();
		
		destMatrix = translationMatrix(-0.5f, 0f, destMatrix);

	}

	public void draw() {
		update();
		
		shader.enable();
		// set the attributes
		shader.setAttribute("a_position", vertexBuffer);
		shader.setAttribute("a_colour", colourBuffer);
		
		// set the uniform to the TRS destination Matrix
		shader.setUniform("u_modelMatrix", destMatrix);

		// draw using index buffer
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexBuffer);
		
		glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
		glDrawElements(GL_TRIANGLES, indices.length, GL_UNSIGNED_INT, 0);
		
	}

	/**
	 * Set the destination matrix to a translation matrix. Note the destination
	 * matrix must already be allocated.
	 * 
	 * @param tx   Offset in the x direction
	 * @param ty   Offset in the y direction
	 * @param dest Destination matrix to write into
	 * @return
	 */
	
	private void update() {
		// calculate seconds since last frame
		long time = System.currentTimeMillis();
		float deltaTime = (time - oldTime) / 1000f;
		oldTime = time;
		
		//System.out.println("update: dt = " + deltaTime + "s");
		
		// increase the rotation angle (in radians) by the speed by deltaTime
		currentRotation += ROTATION_SPEED * deltaTime;
		
		// scale updates by deltaTime
		destMatrix = moveForward(TRANSLATION_SPEED, currentRotation, destMatrix, deltaTime);
		destMatrix = rotationMatrix(currentRotation, destMatrix);
		
		// scale is not affected by delta time
		destMatrix = scaleMatrix(0.1f, 0.1f, destMatrix);
	}
	
	public static Matrix4f moveForward(float speed, float rotation, Matrix4f dest, float dt) {
		
		// rotation is in radians
		
		//      [ 1 0 0 x ]
		// MV = [ 0 1 0 y ]
	    //      [ 0 0 0 0 ]
		//      [ 0 0 0 1 ]
		
		float x = dest.m30() + speed * -(float)Math.sin(rotation) * dt;
		float y = dest.m31() + speed * 	(float)Math.cos(rotation) * dt;
		
		dest = translationMatrix(x, y, dest);
		
		return dest;
	}

	public static Matrix4f translationMatrix(float tx, float ty, Matrix4f dest) {
		
		//dest.identity();
		
		//     [ 1 0 0 tx ]
		// T = [ 0 1 0 ty ]
	    //     [ 0 0 0 0  ]
		//     [ 0 0 0 1  ]

		// Perform operations on only the x and y values of the T vec. 
		// Leaves the z value alone, as we are only doing 2D transformations.
		
		dest.m30(tx);
		dest.m31(ty);

		return dest;
	}

	/**
	 * Set the destination matrix to a rotation matrix. Note the destination matrix
	 * must already be allocated.
	 *
	 * @param angle Angle of rotation (in radians)
	 * @param dest  Destination matrix to write into
	 * @return
	 */

	public static Matrix4f rotationMatrix(float angle, Matrix4f dest) {
		//     [ cos -sin  0 0 ]
		// R = [ sin  cos  0 0 ]
	    //     [ 0    0    0 0 ]
		//     [ 0    0    0 1 ]
		
		dest.m00((float)  Math.cos(angle));
		dest.m10((float)  -Math.sin(angle));
		dest.m01((float)  Math.sin(angle));
		dest.m11((float)  Math.cos(angle));

		return dest;
	}

	/**
	 * Set the destination matrix to a scale matrix. Note the destination matrix
	 * must already be allocated.
	 *
	 * @param sx   Scale factor in x direction
	 * @param sy   Scale factor in y direction
	 * @param dest Destination matrix to write into
	 * @return
	 */

	public static Matrix4f scaleMatrix(float sx, float sy, Matrix4f dest) {
		//     [ sx 0  0 0 ]
		// S = [ 0  sy 0 0 ]
	    //     [ 0  0  0 0 ]
		//     [ 0  0  0 1 ]
		
		dest.m00(dest.m00() * sx);
		dest.m01(dest.m01() * sx);
		dest.m10(dest.m10() * sy);
		dest.m11(dest.m11() * sy);

		return dest;
	}

}
