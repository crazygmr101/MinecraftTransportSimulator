package minecrafttransportsimulator.rendering.instances;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityRoad;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityRoad.RoadComponent;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityRoad.RoadLane;
import minecrafttransportsimulator.mcinterface.MasterLoader;
import minecrafttransportsimulator.rendering.components.OBJParser;

public class RenderRoad extends ARenderTileEntityBase<TileEntityRoad>{
	private static final Map<TileEntityRoad, Map<RoadComponent, Integer>> roadDisplayListMap = new HashMap<TileEntityRoad, Map<RoadComponent, Integer>>();
	
	@Override
	public void render(TileEntityRoad road, float partialTicks){
		//Render road components.
		//First set helper variables.
		Point3d position = new Point3d(0, 0, 0);
		Point3d rotation = new Point3d(0, 0, 0);
		
		//If we haven't rendered the road yet, do so now.
		//We cache it in a DisplayList, as there are a LOT of transforms done each component.
		if(!roadDisplayListMap.containsKey(road)){
			roadDisplayListMap.put(road, new HashMap<RoadComponent, Integer>());
		}
		Map<RoadComponent, Integer> displayListMap = roadDisplayListMap.get(road);
		for(RoadComponent component : road.components.keySet()){
			if(!displayListMap.containsKey(component)){
				int displayListIndex = GL11.glGenLists(1);
				GL11.glNewList(displayListIndex, GL11.GL_COMPILE);
				switch(component){
					case CORE: {
						MasterLoader.renderInterface.bindTexture(road.components.get(component).definition.getTextureLocation());
						//Core components need to be transformed to wedges.
						List<Float[]> transformedVertices = new ArrayList<Float[]>();
						Map<String, Float[][]> parsedModel = OBJParser.parseOBJModel(road.components.get(component).definition.getModelLocation());
						
						Point3d priorPosition = new Point3d(0, 0, 0);
						Point3d priorRotation = new Point3d(0, 0, 0);
						Point3d rotationDelta = new Point3d(0, 0, 0);
						float priorIndex = 0;
						
						GL11.glBegin(GL11.GL_TRIANGLES);
						for(float currentIndex=1; currentIndex<=road.curve.pathLength; ++currentIndex){
							//Copy the master vertices to our transformed ones.
							transformedVertices.clear();
							for(Float[][] vertexSet : parsedModel.values()){
								for(Float[] vertex : vertexSet){
									transformedVertices.add(new Float[]{vertex[0], vertex[1], vertex[2], vertex[3], vertex[4], vertex[5], vertex[6], vertex[7]});
								}
							}
							
							//Get current and prior curve position and rotation.
							//From this, we know how much to stretch the model to that point's rendering area.
							road.curve.setPointToPositionAt(priorPosition, priorIndex);
							road.curve.setPointToRotationAt(priorRotation, priorIndex);
							road.curve.setPointToPositionAt(position, currentIndex);
							road.curve.setPointToRotationAt(rotation, currentIndex);
							
							//If we are a really sharp curve, we might have inverted our model at the inner corner.
							//Check for this, and if we have done so, skip this segment.
							//If we detect this in the last 3 segments, skip right to the end.
							//This prevents a missing end segment due to collision.
							rotationDelta.setTo(rotation).subtract(priorRotation);
							Point3d testPoint1 = new Point3d(road.definition.general.borderOffset, 0, 0).rotateFine(priorRotation).add(priorPosition);
							Point3d testPoint2 = new Point3d(road.definition.general.borderOffset, 0, 0).rotateFine(rotation).add(position);
							if(currentIndex != road.curve.pathLength && (position.x - priorPosition.x)*(testPoint2.x - testPoint1.x) < 0 || (position.z - priorPosition.z)*(testPoint2.z - testPoint1.z) < 0){
								if(currentIndex != road.curve.pathLength && currentIndex + 3 > road.curve.pathLength){
									currentIndex = road.curve.pathLength - 1;
								}
								continue;
							}
							
							//Depending on the vertex position in the model, transform it to match with the offset rotation.
							//This depends on how far the vertex is from the origin of the model, and how big the delta is.
							//For all points, their magnitude depends on how far away they are on the Z-axis.
							for(Float[] vertex : transformedVertices){
								Point3d vertexOffsetPrior = new Point3d(vertex[0], vertex[1], 0);
								vertexOffsetPrior.rotateFine(priorRotation).add(priorPosition);
								Point3d vertexOffsetCurrent = new Point3d(vertex[0], vertex[1], vertex[2]);
								vertexOffsetCurrent.rotateFine(rotation).add(position);
								
								Point3d segmentVector = vertexOffsetPrior.copy().subtract(vertexOffsetCurrent).multiply(Math.abs(vertex[2]));
								Point3d renderedVertex = vertexOffsetCurrent.copy().add(segmentVector).add(road.startingOffset);
								
								GL11.glTexCoord2f(vertex[3], vertex[4]);
								GL11.glNormal3f(vertex[5], vertex[6], vertex[7]);
								GL11.glVertex3d(renderedVertex.x, renderedVertex.y, renderedVertex.z);
							}
							
							//Set the last index.
							priorIndex = currentIndex;
							
							//If we are at the last index, do special logic to get the very end point.
							if(currentIndex != road.curve.pathLength && currentIndex + 1 > road.curve.pathLength){
								currentIndex -= ((currentIndex + 1) - road.curve.pathLength);
							}
						}
						GL11.glEnd();
					}
					case LEFT_BORDER:
						break;
					case RIGHT_BORDER:
						break;
					case LEFT_MARKING:
						break;
					case CENTER_MARKING:
						break;
					case RIGHT_MARKING:
						break;
					case SUPPORT:
						break;
					case UNDERLAYMENT:
						break;
					default:
						break;
				}
				GL11.glEndList();
				displayListMap.put(component, displayListIndex);
			}
			
			MasterLoader.renderInterface.bindTexture(road.components.get(component).definition.getTextureLocation());
			GL11.glCallList(displayListMap.get(component));
		}
		
		//FIXME render stuffs.
		//If we are inactive, render road bounds and colliding boxes.
		if(!road.isActive){
			//Render the information hashes.
			//First set states.
			GL11.glDisable(GL11.GL_TEXTURE_2D);
			GL11.glDisable(GL11.GL_LIGHTING);
			GL11.glLineWidth(2);
			GL11.glBegin(GL11.GL_LINES);
			
			//Render the lane start points.
			GL11.glColor3f(1, 0, 0);
			for(RoadLane lane : road.lanes){
				GL11.glVertex3d(lane.startingOffset.x, lane.startingOffset.y, lane.startingOffset.z);
				GL11.glVertex3d(lane.startingOffset.x, lane.startingOffset.y + 5, lane.startingOffset.z);
			}
			
			//Render the curves.
			//First render the actual curve and its outer border bounds.
			GL11.glColor3f(0, 1, 0);
			for(float f=0; f<road.curve.pathLength; f+=0.1){
				road.curve.setPointToPositionAt(position, f);
				position.add(road.startingOffset);
				GL11.glVertex3d(position.x, position.y, position.z);
				GL11.glVertex3d(position.x, position.y + 1.5, position.z);
				
				road.curve.setPointToRotationAt(rotation, f);
				position.set(road.definition.general.borderOffset, 0, 0).rotateFine(rotation);
				road.curve.offsetPointbyPositionAt(position, f);
				position.add(road.startingOffset);
				
				GL11.glVertex3d(position.x, position.y, position.z);
				GL11.glVertex3d(position.x, position.y + 1.5, position.z);
			}
			
			//Now render the lane curve segments.
			GL11.glColor3f(1, 1, 0);
			for(float laneOffset : road.definition.general.laneOffsets){
				for(float f=0; f<road.curve.pathLength; f+=0.1){
					road.curve.setPointToRotationAt(rotation, f);
					position.set(laneOffset, 0, 0).rotateFine(rotation);
					road.curve.offsetPointbyPositionAt(position, f);
					position.add(road.startingOffset);
					
					GL11.glVertex3d(position.x, position.y, position.z);
					GL11.glVertex3d(position.x, position.y + 1.5, position.z);
				}	
			}
			
			//Set states back to normal.
			GL11.glEnd();
			GL11.glColor3f(1, 1, 1);
			GL11.glEnable(GL11.GL_LIGHTING);
			GL11.glEnable(GL11.GL_TEXTURE_2D);
		}
		
		
		/*
		TileEntityRoad_Core coreComponent = (TileEntityRoad_Core) tile.components.get(Axis.NONE);
		if(coreComponent != null){
			//If we don't have the model parsed, do so now.
			if(!connectorDisplayListMap.containsKey(tile.definition)){
				Map<String, Float[][]> parsedModel = OBJParser.parseOBJModel(tile.definition.getModelLocation());
				
				Map<Axis, Integer> connectorDisplayLists = new HashMap<Axis, Integer>();
				Map<Axis, Integer> solidConncectorDisplayLists = new HashMap<Axis, Integer>();
				for(Axis axis : Axis.values()){
					if(parsedModel.containsKey(axis.name().toLowerCase())){
						connectorDisplayLists.put(axis, cacheAxisVertices(parsedModel.get(axis.name().toLowerCase())));
					}
					if(parsedModel.containsKey(axis.name().toLowerCase() + "_solid")){
						solidConncectorDisplayLists.put(axis, cacheAxisVertices(parsedModel.get(axis.name().toLowerCase() + "_solid")));
					}
				}
				connectorDisplayListMap.put(tile.definition, connectorDisplayLists);
				solidConnectorDisplayListMap.put(tile.definition, solidConncectorDisplayLists);
			}
			
			//Render the connectors.  Don't do this on the blending pass 1.
			if(MasterLoader.renderInterface.getRenderPass() != 1){
				MasterLoader.renderInterface.bindTexture(tile.definition.getTextureLocation());
				for(Axis axis : Axis.values()){
					if(axis.equals(Axis.NONE)){
						GL11.glCallList(connectorDisplayListMap.get(tile.definition).get(axis));
					}else{
						Point3i offset = axis.getOffsetPoint(tile.position);
						boolean adjacentPole = tile.world.getBlock(offset) instanceof BlockPole;
						boolean solidBlock = tile.world.isBlockSolid(offset);
						boolean slabBlock = (axis.equals(Axis.DOWN) && tile.world.isBlockBottomSlab(offset)) || (axis.equals(Axis.UP) && tile.world.isBlockTopSlab(offset));
						if(adjacentPole || solidBlock){
							if(connectorDisplayListMap.get(tile.definition).containsKey(axis)){
								GL11.glCallList(connectorDisplayListMap.get(tile.definition).get(axis));
							}
						}
						if(solidBlock){
							if(solidConnectorDisplayListMap.get(tile.definition).containsKey(axis)){
								GL11.glCallList(solidConnectorDisplayListMap.get(tile.definition).get(axis));
							}
						}else if(slabBlock){
							//Slab.  Render the center and proper portion and center again to render at slab height.
							//Also render solid portion as it's a solid block.
							Axis oppositeAxis = axis.getOpposite();
							if(connectorDisplayListMap.get(tile.definition).containsKey(axis)){
								GL11.glCallList(connectorDisplayListMap.get(tile.definition).get(axis));
								//Offset to slab block.
								GL11.glTranslatef(0.0F, axis.yOffset, 0.0F);
								
								//Render upper and center section.  Upper joins lower above slab.
								if(connectorDisplayListMap.get(tile.definition).containsKey(oppositeAxis)){
									GL11.glCallList(connectorDisplayListMap.get(tile.definition).get(oppositeAxis));
								}
								GL11.glCallList(connectorDisplayListMap.get(tile.definition).get(Axis.NONE));
								
								//Offset to top of slab and render solid lower connector, if we have one.
								GL11.glTranslatef(0.0F, -axis.yOffset/2F, 0.0F);
								if(solidConnectorDisplayListMap.get(tile.definition).containsKey(axis)){
									GL11.glCallList(solidConnectorDisplayListMap.get(tile.definition).get(axis));
								}
								
								//Translate back to the normal position.
								GL11.glTranslatef(0.0F, -axis.yOffset/2F, 0.0F);
							}
						}
					}
				}
			}
		}
		
		//Done rendering core and connections.  Render components now.
		for(Axis axis : Axis.values()){
			if(!axis.equals(Axis.NONE)){
				if(tile.components.containsKey(axis)){
					//Cache the displaylists and lights if we haven't already.
					ATileEntityPole_Component component = tile.components.get(axis);
					if(!componentDisplayListMap.containsKey(component.definition)){
						Map<String, Float[][]> parsedModel = OBJParser.parseOBJModel(component.definition.getModelLocation());
						List<TransformLight> lightParts = new ArrayList<TransformLight>();
						int displayListIndex = GL11.glGenLists(1);
						GL11.glNewList(displayListIndex, GL11.GL_COMPILE);
						GL11.glBegin(GL11.GL_TRIANGLES);
						for(Entry<String, Float[][]> entry : parsedModel.entrySet()){
							if(entry.getKey().startsWith("&")){
								//Save light for special rendering.
								lightParts.add(new TransformLight(component.definition.general.modelName, entry.getKey(), entry.getValue()));
								if(lightParts.get(lightParts.size() - 1).isLightupTexture){
									continue;
								}
							}
							//Add vertices
							for(Float[] vertex : entry.getValue()){
								GL11.glTexCoord2f(vertex[3], vertex[4]);
								GL11.glNormal3f(vertex[5], vertex[6], vertex[7]);
								GL11.glVertex3f(vertex[0], vertex[1], vertex[2]);
							}
						}
						GL11.glEnd();
						GL11.glEndList();
						
						//Put parsed model into the maps.
						componentDisplayListMap.put(component.definition, displayListIndex);
						componentLightMap.put(component.definition, lightParts);
					}
					
					//Rotate to component axis and render.
					GL11.glPushMatrix();
					GL11.glRotatef(axis.yRotation, 0, 1, 0);
					GL11.glTranslatef(0, 0, tile.definition.general.radius + 0.001F);
					
					//Don't do solid model rendering on the blend pass.
					if(MasterLoader.renderInterface.getRenderPass() != 1){
						MasterLoader.renderInterface.bindTexture(component.definition.getTextureLocation());
						GL11.glCallList(componentDisplayListMap.get(component.definition));
					}
					
					if(component instanceof TileEntityPole_TrafficSignal){
						LightType litLight;
						switch(((TileEntityPole_TrafficSignal) component).state){
							case UNLINKED: litLight = LightType.UNLINKEDLIGHT; break;
							case RED: litLight = LightType.STOPLIGHT; break;
							case YELLOW: litLight = LightType.CAUTIONLIGHT; break;
							case GREEN: litLight = LightType.GOLIGHT; break;
							default: litLight = null;
						}
						for(TransformLight lightPart : componentLightMap.get(component.definition)){
							lightPart.renderOnBlock(tile.world, tile.position, lightPart.type.equals(litLight));
						}
					}else if(component instanceof TileEntityPole_StreetLight){
						for(TransformLight lightPart : componentLightMap.get(component.definition)){
							lightPart.renderOnBlock(tile.world, tile.position, ((TileEntityPole_StreetLight) component).state.equals(LightState.ON));
						}
					}else if(component instanceof TileEntityPole_Sign){
						//Render lights, if we have any.
						for(TransformLight lightPart : componentLightMap.get(component.definition)){
							lightPart.renderOnBlock(tile.world, tile.position, true);
						}
						
						//Render text, if we have any.
						if(component.definition.general.textObjects != null){
							MasterLoader.renderInterface.renderTextMarkings(component.definition.general.textObjects, ((TileEntityPole_Sign) component).getTextLines(), null, null, false);
						}
					}
					GL11.glPopMatrix();
				}
			}
		}*/
	}
	
	@Override
	public boolean rotateToBlock(){
		return false;
	}
	
	@Override
	public boolean translateToSlabs(){
		return false;
	}
}
