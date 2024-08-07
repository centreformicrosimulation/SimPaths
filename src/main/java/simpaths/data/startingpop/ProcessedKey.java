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

    @Column(name="id") private long id = 1L;
    @Column(name="simulation_time") private double simulationTime;
    @Column(name="simulation_run") private long simulationRun;
    @Column(name="processed_id") private long processedId = 0L;


    public ProcessedKey(){
        super();
    }

    public ProcessedKey(long id){
        super();
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public double getSimulationTime() {
        return simulationTime;
    }

    public void setSimulationTime(Double simulationTime) {
        this.simulationTime = simulationTime;
    }

    public long getSimulationRun() {
        return simulationRun;
    }

    public void setSimulationRun(long simulationRun) {
        this.simulationRun = simulationRun;
    }

    public long getProcessedId() { return processedId; }

    public void setProcessedId(long processedId) { this.processedId = processedId; }
}
