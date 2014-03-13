/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2012-2014 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.wkf.helper

import groovy.util.logging.Slf4j;

import javax.inject.Inject
import javax.xml.parsers.ParserConfigurationException

import org.xml.sax.SAXException

import wslite.json.JSONException
import wslite.json.JSONObject

import com.axelor.wkf.db.Instance

@Slf4j
class DiagramHelper {
	
	@Inject
	protected XmlParser xparse
	
	public String generateDiagram (Instance instance) {
			
		log.debug("Generate diagram for instance ::: {}", instance)
		
		try {
			
			String xmlData = instance.workflow.xmlData
			
			NodeList svgList = (NodeList) xparse.parseText(xmlData)["svg-representation"]
			NodeList jsonList = (NodeList) xparse.parseText(xmlData)["json-representation"]
			
			String svgNode =  ( (NodeList) ( (Node) svgList[0] ).value() )[0]
			String jsonNode = ((NodeList) ( (Node) jsonList[0] ).value() )[0]
			
			return updateDiagram( svgNode, jsonNode, instance.nodes.collect { it.name } )
			
		}
		catch(Exception e){
			
			log.error("${e}")
			return null
			
		}
	}
	
	protected String updateDiagram (String svgNode, String jsonNode, Collection activeNodes) throws Exception {
		
		log.debug("Update diagram for active nodes ::: {}", activeNodes)
		
		JSONObject diagram = new JSONObject(jsonNode)
		def svg = xparse.parseText(svgNode)
		
		for ( String activeNode : activeNodes ) {
			
			for( JSONObject shape : diagram.get("childShapes") ){
				
				if( shape["properties"]["name"] == activeNode ){
					
					def nd = svg.depthFirst().find{ node -> node.attribute('id') == "svg-${shape['resourceId']}" }
					
					def children = nd.depthFirst().findAll{ node -> node.attribute('stroke') != null }
					
					for(child in children) { child.attributes().put("stroke","red") }
					
					break;
					
				}
			}
		}
		
		StringWriter so = new StringWriter()
		PrintWriter po = new PrintWriter(so)	
		XmlNodePrinter pn = new XmlNodePrinter(po)
		pn.print(svg)
		return so.toString()
		
	}
	
}
