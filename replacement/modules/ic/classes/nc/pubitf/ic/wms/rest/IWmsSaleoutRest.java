package nc.pubitf.ic.wms.rest;

import java.io.IOException;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import org.json.JSONString;

import com.alibaba.fastjson.JSONObject;
import nc.vo.scmpub.res.Module;
import uap.ws.rest.resource.AbstractUAPRestResource;

@Path("/IWmsSaleoutRest")
public class IWmsSaleoutRest extends AbstractUAPRestResource {

	@Override
	public String getModule() {
		// TODO Auto-generated method stub
		return Module.IC.getName();
	}

  @POST
  @Path("/WmsDeliveryTo4C")
  @Consumes("application/json")
  @Produces("application/json")
  public JSONString WmsDeliveryTo4C(JSONObject  str){
	  WmsSaleoutNCRestImpl restimpl = new WmsSaleoutNCRestImpl();
	  return restimpl.wmsDeliveryTo4C(str);
  }
  
  @POST
  @Path("/WmsSaleoutTo4C")
  @Consumes("application/json")
  @Produces("application/json")
  public JSONString WmsSaleoutTo4C(JSONObject  str){
	  WmsSaleoutNCRestImpl restimpl = new WmsSaleoutNCRestImpl();
	  return restimpl.wmsSaleoutTo4C(str);
  }
  
  @POST
  @Path("/WmsUpdateM4R")
  @Consumes("application/json")
  @Produces("application/json")
  public JSONString WmsUpdateM4R(JSONObject  str){
	  WmsSaleoutNCRestImpl restimpl = new WmsSaleoutNCRestImpl();
	  return restimpl.WmsUpdateM4R(str);
  }
  
  
  @POST
  @Path("/WmsInsertM4N")
  @Consumes("application/json")
  @Produces("application/json")
  public JSONString WmsInsertM4N(JSONObject  str){
	  WmsSaleoutNCRestImpl restimpl = new WmsSaleoutNCRestImpl();
	  return restimpl.WmsInsertM4N(str);
  }
  
  @POST
  @Path("/WmsAddM4nBody")
  @Consumes("application/json")
  @Produces("application/json")
  public JSONString WmsAddM4nBody(JSONObject  str){
	  WmsSaleoutNCRestImpl restimpl = new WmsSaleoutNCRestImpl();
	  return restimpl.WmsAddM4nBody(str);
  }
  
  @POST
  @Path("/FilePostToSaleOut")
  @Consumes("application/json")
  @Produces("application/json")
  public JSONString FilePostToSaleOut(JSONObject str) throws IOException{
	  WmsSaleoutNCRestImpl restimpl = new WmsSaleoutNCRestImpl();
	  return restimpl.FilePostToSaleOut(str);
  }
  
//  @POST
//  @Path("/editSaleOut")
//  @Consumes("application/json")
//  @Produces("application/json")
//  public JSONString editSaleOut(JSONObject str){
//	  WmsSaleoutNCRestImpl restimpl = new WmsSaleoutNCRestImpl();
//	  return restimpl.editSaleOut(str);
//  }

}
