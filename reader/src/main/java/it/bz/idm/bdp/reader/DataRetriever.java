package it.bz.idm.bdp.reader;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import it.bz.idm.bdp.dal.DataType;
import it.bz.idm.bdp.dal.Station;
import it.bz.idm.bdp.dal.authentication.BDPRole;
import it.bz.idm.bdp.dal.bluetooth.Linkstation;
import it.bz.idm.bdp.dal.parking.ParkingStation;
import it.bz.idm.bdp.dal.util.JPAUtil;
import it.bz.idm.bdp.dto.ChildDto;
import it.bz.idm.bdp.dto.RecordDto;
import it.bz.idm.bdp.dto.StationDto;
import it.bz.idm.bdp.dto.TypeDto;
import it.bz.idm.bdp.dto.parking.ParkingRecordDto;
import it.bz.idm.bdp.dto.parking.ParkingStationDto;

public class DataRetriever {

	//Utility
	private List<String> getStationTypes(EntityManager em){
		List<String> result = null;
		result = Station.findStationTypes(em);
		return result;

	}

	//API additions for V2
	public List<ChildDto> getChildren(String type, String parent){
		EntityManager em = JPAUtil.createEntityManager();
		try{
			Station station = (Station) JPAUtil.getInstanceByType(em, type);
			List<ChildDto> children = null;
			if (station != null){
				children = station.findChildren(em,parent);
				return children;
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}finally{
			em.close();
		}
		return null;
	}
	public List<TypeDto> getTypes(String type, String stationId) {
		List<TypeDto> dataTypes = null;
		EntityManager em = JPAUtil.createEntityManager();
		try{
			Station station = (Station) JPAUtil.getInstanceByType(em, type);
			if (station!=null)
				dataTypes = station.findTypes(em, stationId);
		}catch(Exception ex){
			ex.printStackTrace();
		}finally{
			em.close();
		}
		return dataTypes;
	}

	//APIv1

	public List<? extends StationDto> getStationDetails(String type, String id) {
		List<StationDto> stations = new ArrayList<StationDto>();
		EntityManager em = JPAUtil.createEntityManager();
		try{
			Station station = (Station) JPAUtil.getInstanceByType(em, type);
			Station stationById = null;
			if (id != null && !id.isEmpty()) {
				stationById = station.findStation(em,id);
			}
			if ((stationById == null && (id == null || id.isEmpty())) || (stationById != null && station.getClass().equals(stationById.getClass())))
				stations = station.findStationsDetails(em,stationById);
		}catch(Exception ex){
			ex.printStackTrace();
		}finally{
			em.close();
		}
		return stations;
	}

	public List<String> getStations(String type){
		List<String> stations = null;
		EntityManager em = JPAUtil.createEntityManager();
		try{
			if (getStationTypes(em).contains(type))
				stations = Station.findActiveStations(em,type);
		}catch(Exception ex){
			ex.printStackTrace();
		}finally{
			em.close();
		}
		return stations;
	}


	public List<StationDto> getAvailableStations(){
		List<StationDto> availableStations = new ArrayList<StationDto>();
		EntityManager em = JPAUtil.createEntityManager();
		try{
			availableStations = new Linkstation().findAvailableStations(em);

		}catch(Exception ex){
			ex.printStackTrace();
		}finally{
			em.close();
		}
		return availableStations;
	}

	public List<String[]> getDataTypes(String type, String stationId) {
		List<String[]> dataTypes = null;
		EntityManager em = JPAUtil.createEntityManager();
		try{
			Station station = (Station) JPAUtil.getInstanceByType(em, type);
			if (station != null)
				dataTypes = station.findDataTypes(em,stationId);
		}catch(Exception ex){
			ex.printStackTrace();
		}finally{
			em.close();
		}
		return dataTypes;
	}

	public List<String[]> getDataTypes(String type){
		return getDataTypes(type, null);
	}

	public Date getDateOfLastRecord(String stationTypology, String stationcode, String cname, Integer period) {
		EntityManager em = JPAUtil.createEntityManager();
		BDPRole role = BDPRole.fetchGuestRole(em);
		Date dateOfLastRecord = null;
		try{
			Station s = (Station) JPAUtil.getInstanceByType(em, stationTypology);
			Station station = s.findStation(em, stationcode);
			DataType type = DataType.findByCname(em,cname);
			dateOfLastRecord = s.getDateOfLastRecord(em, station, type, period, role);
		}catch(Exception ex){
			ex.printStackTrace();
		}finally{
			em.close();
		}
		return dateOfLastRecord;
	}

	public RecordDto getLastRecord(String stationTypology, String stationcode, String cname, Integer period) {
		ParkingRecordDto dto = null;
		EntityManager em = JPAUtil.createEntityManager();
		BDPRole role = BDPRole.fetchGuestRole(em);
		try{
			Station s = (Station) JPAUtil.getInstanceByType(em, stationTypology);
			Station station = s.findStation(em, stationcode);
			dto = (ParkingRecordDto) station.findLastRecord(em, cname, period, role);
		}catch(Exception ex){
			ex.printStackTrace();
		}finally{
			em.close();
		}
		return dto;
	}
	public RecordDto getNewestRecord(String typology, String stationId, String typeId, Integer period) {
		RecordDto dto = null;
		EntityManager em = JPAUtil.createEntityManager();
		BDPRole role = BDPRole.fetchGuestRole(em);
		try{
			Station s = (Station) JPAUtil.getInstanceByType(em, typology);
			if (s != null){
				Station station = s.findStation(em, stationId);
				dto = station.findLastRecord(em, typeId, period, role);
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}finally{
			em.close();
		}
		return dto;
	}
	public List<RecordDto> getRecords(String stationtypology,String identifier, String type, Integer seconds, Integer period){
		Date end = new Date();
		Date start = new Date(end.getTime()-(seconds*1000l));
		return getRecords(stationtypology, identifier, type, start, end, period,seconds);
	}

	public List<RecordDto> getRecords(String stationtypology, String identifier, String type, Date start, Date end,
			Integer period, Integer seconds) {
		if (seconds != null && (start == null && end ==null )) {
			end = new Date();
			start = new Date(end.getTime()-(seconds*1000l));
		}
		EntityManager em = JPAUtil.createEntityManager();
		BDPRole role = BDPRole.fetchGuestRole(em);
		List<RecordDto> records = new ArrayList<RecordDto>();
		try{
			Station s = (Station) JPAUtil.getInstanceByType(em, stationtypology);
			Station station = s.findStation(em, identifier);
			if (station != null) {
				records.addAll(station.getRecords(em, type, start, end, period, role));
				return records;
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}
		finally{
			em.close();
		}
		return records;
	}

	//LEGACY API FOR PARKING
	@Deprecated
	public List<String> fetchParkingIds(){
		EntityManager em = JPAUtil.createEntityManager();
		List<String> stations = Station.findActiveStations(em,"ParkingStation");
		em.close();
		return stations ;
	}
	@Deprecated
	public Map<String,Object> fetchParkingStation(String identifier){
		return ParkingStation.findParkingStation(identifier);
	}

	@Deprecated
	public List<ParkingStationDto> fetchParkingStations(){
		return ParkingStation.findParkingStationsMetadata();
	}
	@Deprecated
	public Integer getNumberOfFreeSlots(String identifier){
		return ParkingStation.findNumberOfFreeSlots(identifier);
	}
	@Deprecated
	public List<Object[]> fetchStoricData(String identifier,Integer minutes){
		return ParkingStation.findStoricData(identifier,minutes);

	}
	@Deprecated
	public List<Object> fetchFreeSlotsByTimeFrame(String identifier, String startDateString, String endDateString,
			String datePattern) {
		return ParkingStation.findFreeSlotsByTimeFrame(identifier,startDateString, endDateString,datePattern);
	}

}