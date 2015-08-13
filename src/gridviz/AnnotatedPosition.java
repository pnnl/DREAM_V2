/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gridviz;

import javax.vecmath.Point2i;
import javax.vecmath.Point3i;
import javax.vecmath.Vector3f;

/**
 * @brief  Describes a point in 3d space with a position and a text annotation
 * @author d3x874
 */
public class AnnotatedPosition {
	
	/// Provides an annotation text for the position
	private String _annotation;
	
	/// Describes a real valued position in 3 space
	private Vector3f _position;
	
	/// Describes the ijks a node
	private Point3i _ijkLocation;

	/// The position of the annotation on the slice image (y=i=row, x=j=col)
	private Point2i _slicePosition;
	
	/// Indicates if this position intersects the slice
	private boolean _intersectsSlice;
	
	/// Constructs and Annotated Position
	public AnnotatedPosition(
			Vector3f position, ///< The position in 3d space of the point
			String annotation  ///< The text annotation for the point
			) {
		_position = position;
		_annotation = annotation;
	}
	
	
	public AnnotatedPosition(
			Point3i ijks, ///< The position in 3d space of the point
			String annotation  ///< The text annotation for the point
			) {
		_ijkLocation = ijks;
		_annotation = annotation;
	}
	
	
	// Getters and setters for the properties:
	
	public boolean getIntersectsSlice() {
		return _intersectsSlice;
	}

	public void setIntersectsSlice(boolean intersectsSlice) {
		_intersectsSlice = intersectsSlice;
	}

	public Point2i getSlicePosition() {
		return _slicePosition;
	}

	public void setSlicePosition(Point2i slicePosition) {
		_slicePosition = slicePosition;
	}
	
	public String getAnnotation() {
		return _annotation;
	}
	
	public void setAnnoataion(String annotation) {
		_annotation = annotation;
	}
	
	public Vector3f getPosition() {
		return _position;
	}
	
	public void setPosition(Vector3f position) {
		_position = position;
	}


	public Point3i get_ijkLocation() {
		return _ijkLocation;
	}
}
