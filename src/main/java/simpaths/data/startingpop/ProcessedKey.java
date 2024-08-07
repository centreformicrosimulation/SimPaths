package simpaths.data.startingpop;


import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import microsim.data.db.PanelEntityKey;
import simpaths.model.enums.Country;

import java.io.Serializable;

@Embeddable
public class ProcessedKey implements Serializable {

    PanelEntityKey key;
    @Column(name = "processed_id") private long processedId = 0L;


    public ProcessedKey(){
        key = new PanelEntityKey();
    }

    public ProcessedKey(long panelEntityKeyId){
        key = new PanelEntityKey(panelEntityKeyId);
    }


    public void setProcessedId(long processedId) {
        this.processedId = processedId;
    }

    public long getId() {
        return key.getId();
    }

    public void setId(long id) {
        key.setId(id);
    }

    public double getSimulationTime() {
        return key.getSimulationTime();
    }

    public void setSimulationTime(Double simulationTime) {
        key.setSimulationTime(simulationTime);
    }

    public long getSimulationRun() {
        return key.getSimulationRun();
    }

    public void setSimulationRun(Long simulationRun) {
        key.setSimulationRun(simulationRun);
    }

}
