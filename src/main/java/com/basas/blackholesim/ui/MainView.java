package com.basas.blackholesim.ui;

import com.basas.blackholesim.core.SimulationEngine;
import com.basas.blackholesim.core.entities.BlackHole;
import com.basas.blackholesim.core.entities.Particle;
import com.basas.blackholesim.core.math.Vec2;
import com.basas.blackholesim.core.physics.*;
import com.basas.blackholesim.render.Camera;
import com.basas.blackholesim.render.CanvasRenderer;
import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.Locale;


public class MainView {

    private final BorderPane root = new BorderPane();

    private final Canvas canvas = new Canvas(860, 720);
    private final SimulationEngine engine = new SimulationEngine();
    private final Camera camera = new Camera();
    private final CanvasRenderer renderer = new CanvasRenderer(engine, camera);

    private final UiState ui = new UiState();

    private AnimationTimer timer;

    
    private boolean panning = false;
    private double lastMouseX;
    private double lastMouseY;

    
    private BlackHole draggingBlackHole = null;

    
    private ComboBox<String> bhSelect;
    private Label bhCountLabel;

    
    private final RungeKutta4Integrator rk4 = new RungeKutta4Integrator();
    private final DormandPrince45Integrator rk45 = new DormandPrince45Integrator();
    private final VelocityVerletIntegrator verlet = new VelocityVerletIntegrator();
    private final RelativisticGeodesicIntegrator geodesic = new RelativisticGeodesicIntegrator();

    public MainView() {
        Locale.setDefault(Locale.US);

        buildLayout();
        setupSimulationDefaults();
        setupInputs();
        setupLoop();
    }

    public Parent getRoot() { return root; }

    public void start() { timer.start(); }

    private void buildLayout() {
        StackPane center = new StackPane(canvas);
        center.setPadding(new Insets(14));
        center.setStyle("-fx-background-color: transparent;");
        root.setCenter(center);

        canvas.widthProperty().bind(center.widthProperty().subtract(28));
        canvas.heightProperty().bind(center.heightProperty().subtract(28));

        VBox controls = buildControlsPane();

        ScrollPane controlsScroll = new ScrollPane(controls);
        controlsScroll.setFitToWidth(true);
        controlsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        controlsScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        controlsScroll.setPannable(true);
        controlsScroll.setPrefWidth(360);
        controlsScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        root.setRight(controlsScroll);

        HBox top = buildTopBar();
        root.setTop(top);
    }

    private HBox buildTopBar() {
        Label title = new Label("Black Hole Simulator — Professional Physics Mode");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: 700;");

        Label subtitle = new Label("PW potential • Newtonian • Verlet/RK4/RK45 • Energy drift diagnostics");
        subtitle.getStyleClass().add("small-muted");

        VBox text = new VBox(title, subtitle);
        text.setSpacing(2);

        Button resetView = new Button("Reset view");
        resetView.setOnAction(e -> resetCamera());

        Button resetPreset = new Button("Reset preset");
        resetPreset.setOnAction(e -> applyPreset("Accretion disk"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bar = new HBox(text, spacer, resetView, resetPreset);
        bar.setPadding(new Insets(12, 14, 10, 14));
        bar.setSpacing(10);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-border-color: rgba(255,255,255,0.08); -fx-border-width: 0 0 1 0;");
        return bar;
    }

    private VBox buildControlsPane() {
        VBox box = new VBox();
        box.setPadding(new Insets(14));
        box.setSpacing(12);
        box.setPrefWidth(360);
        box.setStyle("-fx-background-color: rgba(255,255,255,0.02); -fx-border-color: rgba(255,255,255,0.08); -fx-border-width: 0 0 0 1;");

        VBox card = new VBox();
        card.getStyleClass().add("card");
        card.setSpacing(10);

        
        Label presetLabel = new Label("Preset");
        ComboBox<String> presets = new ComboBox<>();
        presets.getItems().addAll("Accretion disk", "Stable orbit", "Binary system", "Slingshot");
        presets.getSelectionModel().select("Accretion disk");
        presets.setMaxWidth(Double.MAX_VALUE);
        presets.setOnAction(e -> applyPreset(presets.getValue()));

        
        Label modelLabel = new Label("Gravity model");
        ComboBox<String> model = new ComboBox<>();
        model.getItems().addAll("Paczynski–Wiita (pseudo-GR)", "Newtonian");
        model.getSelectionModel().select(0);
        model.setMaxWidth(Double.MAX_VALUE);
        model.setOnAction(e -> {
            String v = model.getValue();
            if (v.startsWith("Newtonian")) engine.setGravityModel(new NewtonianGravityModel());
            else engine.setGravityModel(new PaczynskiWiitaGravityModel());
        });

        
        Label integLabel = new Label("Integrator");
        ComboBox<String> integ = new ComboBox<>();
        integ.getItems().addAll("Velocity Verlet (fast+stable)", "RK4 (precise)", "RK45 adaptive (very precise)", "Relativistic Geodesics (Schwarzschild/Kerr)");
        integ.getSelectionModel().select(0);
        integ.setMaxWidth(Double.MAX_VALUE);
        integ.setOnAction(e -> {
            String v = integ.getValue();
            if (v.startsWith("RK4")) engine.setIntegrator(rk4);
            else if (v.startsWith("RK45")) engine.setIntegrator(rk45);
            else if (v.startsWith("Relativistic")) engine.setIntegrator(geodesic);
            else engine.setIntegrator(verlet);
        });

        
        Label precisionLabel = new Label("Precision (substeps / tolerance)");
        Slider precision = new Slider(1, 10, 3);
        precision.setShowTickLabels(true);
        precision.setShowTickMarks(true);
        precision.valueProperty().addListener((obs, o, nv) -> {
            int p = (int) Math.round(nv.doubleValue());

            
            rk4.setSubsteps(p);

            
            double t = Math.pow(10, -2.0 - (p - 1) * (3.0 / 9.0));
            rk45.setTolerance(t);
        });

        
        Label metricLabel = new Label("Metric (for geodesics + disk)");
        ComboBox<RelativityMode> metric = new ComboBox<>();
        metric.getItems().addAll(RelativityMode.NEWTONIAN, RelativityMode.SCHWARZSCHILD, RelativityMode.KERR);
        metric.getSelectionModel().select(engine.getParams().relativityMode);
        metric.setMaxWidth(Double.MAX_VALUE);
        metric.setOnAction(e -> engine.getParams().relativityMode = metric.getValue());

        Label spinLabel = new Label("Spin a* (selected BH)");
        Slider spin = new Slider(0.0, 0.999, 0.0);
        spin.setShowTickLabels(true);
        spin.setShowTickMarks(true);
        spin.valueProperty().addListener((obs, o, v) -> {
            BlackHole selected = getSelectedBh();
            if (selected != null) selected.setSpin(v.doubleValue());
        });

        CheckBox disk = new CheckBox("Accretion disk (emissivity + Doppler/redshift)");
        disk.setSelected(engine.getParams().enableAccretionDisk);
        disk.setOnAction(e -> engine.getParams().enableAccretionDisk = disk.isSelected());

        CheckBox bhDyn = new CheckBox("BH dynamics + merge (momentum + GW loss)");
        bhDyn.setSelected(engine.getParams().enableBHDynamics);
        bhDyn.setOnAction(e -> engine.getParams().enableBHDynamics = bhDyn.isSelected());

        Label gwLabel = new Label("GW loss strength");
        Slider gw = new Slider(0.0, 1.0, engine.getParams().gwLossStrength);
        gw.setShowTickLabels(true);
        gw.setShowTickMarks(true);
        gw.valueProperty().addListener((obs, o, v) -> engine.getParams().gwLossStrength = v.doubleValue());

        Button addPhoton = new Button("Add photon");
        Button photonBurst = new Button("Photon burst");

        addPhoton.setOnAction(e -> {
            Vec2 w = camera.screenToWorld(canvas.getWidth() * 0.55, canvas.getHeight() * 0.45);
            Vec2 v = new Vec2(0, -engine.getParams().c);
            Particle p = new Particle(new Vec2(w.x, w.y), v);
            p.setPhoton(true);
            p.setGeodesic(true);
            p.setColor(Color.rgb(255, 255, 255, 0.9));
            p.setRadius(1.8);
            p.setMaxTrailPoints(220);
            engine.addParticle(p);
        });

        photonBurst.setOnAction(e -> {
            Vec2 w = camera.screenToWorld(canvas.getWidth() * 0.55, canvas.getHeight() * 0.45);
            for (int i = 0; i < 140; i++) {
                double a = Math.random() * Math.PI * 2.0;
                Vec2 v = new Vec2(Math.cos(a) * engine.getParams().c, Math.sin(a) * engine.getParams().c);
                Particle p = new Particle(new Vec2(w.x, w.y), v);
                p.setPhoton(true);
                p.setGeodesic(true);
                p.setColor(Color.rgb(255, 255, 255, 0.65));
                p.setRadius(1.4);
                p.setMaxTrailPoints(260);
                engine.addParticle(p);
            }
        });


        
        Label bhLabel = new Label("Selected BH");
        bhSelect = new ComboBox<>();
        bhSelect.setMaxWidth(Double.MAX_VALUE);
        bhSelect.setOnAction(e -> syncMassFromSelectedBh());

        bhCountLabel = new Label("");
        bhCountLabel.getStyleClass().add("small-muted");

        
        Label massLabel = new Label("Mass (selected)");
        Slider mass = new Slider(10, 260, ui.getMass());
        mass.setShowTickLabels(true);
        mass.setShowTickMarks(true);
        mass.valueProperty().addListener((obs, o, v) -> {
            ui.setMass(v.doubleValue());
            BlackHole selected = getSelectedBh();
            if (selected != null) selected.setMass(ui.getMass());
        });

        
        Label cLabel = new Label("c (scale for horizon radius)");
        Slider c = new Slider(20, 140, engine.getParams().c);
        c.setShowTickLabels(true);
        c.setShowTickMarks(true);
        c.valueProperty().addListener((obs, o, v) -> engine.getParams().c = v.doubleValue());

        
        Button placeBh = new Button("Place Black Hole");
        placeBh.setMaxWidth(Double.MAX_VALUE);
        placeBh.setOnAction(e -> ui.setPlaceBlackHoleMode(true));

        Button removeBh = new Button("Remove selected BH");
        removeBh.setMaxWidth(Double.MAX_VALUE);
        removeBh.setOnAction(e -> {
            BlackHole selected = getSelectedBh();
            if (selected == null) return;
            if (engine.getBlackHoles().size() <= 1) return;
            engine.removeBlackHoleById(selected.getId());
            refreshBhSelect();
        });

        
        Label speedLabel = new Label("Simulation speed");
        Slider speed = new Slider(0.10, 4.0, ui.getSpeed());
        speed.setShowTickLabels(true);
        speed.setShowTickMarks(true);
        speed.valueProperty().addListener((obs, o, v) -> ui.setSpeed(v.doubleValue()));

        
        Label zoomLabel = new Label("Zoom");
        Slider zoom = new Slider(0.35, 4.0, ui.getZoom());
        zoom.setShowTickLabels(true);
        zoom.setShowTickMarks(true);
        zoom.valueProperty().addListener((obs, o, v) -> {
            ui.setZoom(v.doubleValue());
            camera.setZoom(ui.getZoom());
        });

        
        ToggleButton pause = new ToggleButton("Pause");
        pause.setSelected(ui.isPaused());
        pause.selectedProperty().addListener((obs, o, v) -> ui.setPaused(v));

        
        ToggleButton trails = new ToggleButton("Trails");
        trails.setSelected(ui.isTrailsEnabled());
        trails.selectedProperty().addListener((obs, o, v) -> ui.setTrailsEnabled(v));

        ToggleButton grid = new ToggleButton("Grid distortion");
        grid.setSelected(ui.isGridDistortionEnabled());
        grid.selectedProperty().addListener((obs, o, v) -> ui.setGridDistortionEnabled(v));

        ToggleButton velVec = new ToggleButton("Velocity vectors");
        velVec.setSelected(ui.isVelocityVectorsEnabled());
        velVec.selectedProperty().addListener((obs, o, v) -> ui.setVelocityVectorsEnabled(v));

        HBox toggles = new HBox(pause, trails, grid);
        toggles.setSpacing(10);

        
        Button addBurst = new Button("Add burst (+200)");
        addBurst.setMaxWidth(Double.MAX_VALUE);
        addBurst.setOnAction(e -> engine.addRandomBurst(new Vec2(0, 0), 200, 300));

        Button addOne = new Button("Add particle");
        addOne.setMaxWidth(Double.MAX_VALUE);
        addOne.setOnAction(e -> {
            Vec2 pos = new Vec2(220, 0);
            Vec2 vel = engine.makeTangentialOrbitVelocity(pos, 1.0);
            engine.addParticle(new Particle(pos, vel));
        });

        Button clear = new Button("Clear particles");
        clear.setMaxWidth(Double.MAX_VALUE);
        clear.setOnAction(e -> engine.clearParticles());

        Label info = new Label(
                "Professional notes:\n" +
                "- Use PW model for strong-field behavior near horizon\n" +
                "- Use RK45 for maximum accuracy (slower)\n" +
                "- Watch 'Energy drift' in HUD to validate dt / integrator\n\n" +
                "Canvas tips:\n" +
                "- Click: spawn particle with tangential velocity\n" +
                "- Place BH mode: click to create a black hole\n" +
                "- Shift+Drag near BH: move it\n" +
                "- Drag RMB/MMB: pan, Wheel: zoom"
        );
        info.setWrapText(true);
        info.getStyleClass().add("small-muted");

        card.getChildren().addAll(
                presetLabel, presets,
                new Separator(),
                modelLabel, model,
                integLabel, integ,
                precisionLabel, precision,
                new Separator(),
                metricLabel, metric,
                spinLabel, spin,
                disk,
                bhDyn,
                gwLabel, gw,
                new HBox(10, addPhoton, photonBurst),
                new Separator(),
                bhLabel, bhSelect, bhCountLabel,
                massLabel, mass,
                cLabel, c,
                placeBh, removeBh,
                new Separator(),
                speedLabel, speed,
                zoomLabel, zoom,
                toggles,
                velVec,
                new Separator(),
                addBurst, addOne, clear,
                new Separator(),
                info
        );

        box.getChildren().add(card);
        return box;
    }

    private void setupSimulationDefaults() {
        resetCamera();
        applyPreset("Accretion disk");
    }

    private void resetCamera() {
        camera.setCenterWorld(0, 0);
        camera.setZoom(ui.getZoom());
    }

    private void refreshBhSelect() {
        bhSelect.getItems().clear();
        for (BlackHole bh : engine.getBlackHoles()) {
            bhSelect.getItems().add(bh.getId());
        }
        if (!bhSelect.getItems().isEmpty()) bhSelect.getSelectionModel().select(0);
        syncMassFromSelectedBh();
        bhCountLabel.setText("Black holes: " + engine.getBlackHoles().size());
    }

    private BlackHole getSelectedBh() {
        if (bhSelect == null) return null;
        String id = bhSelect.getSelectionModel().getSelectedItem();
        if (id == null) return null;

        for (BlackHole bh : engine.getBlackHoles()) {
            if (bh.getId().equals(id)) return bh;
        }
        return null;
    }

    private void syncMassFromSelectedBh() {
        BlackHole selected = getSelectedBh();
        if (selected != null) ui.setMass(selected.getMass());
    }

    private void applyPreset(String preset) {
        engine.clearParticles();
        engine.clearBlackHoles();

        switch (preset) {
            case "Stable orbit" -> {
                engine.addBlackHole(new BlackHole("BH-1", new Vec2(0, 0), 90));
                Vec2 p = new Vec2(320, 0);
                Vec2 v = engine.makeTangentialOrbitVelocity(p, 1.0);
                Particle single = new Particle(p, v);
                single.setMaxTrailPoints(160);
                engine.addParticle(single);
                engine.addRandomBurst(new Vec2(0, 0), 80, 380);
            }
            case "Binary system" -> {
                BlackHole b1 = new BlackHole("BH-1", new Vec2(-170, 0), new Vec2(0, 135), 80, 0.2);
                BlackHole b2 = new BlackHole("BH-2", new Vec2(170, 0), new Vec2(0, -135), 80, 0.2);
                engine.addBlackHole(b1);
                engine.addBlackHole(b2);
                engine.addRandomBurst(new Vec2(0, 0), 320, 520);
            }
            case "Slingshot" -> {
                engine.addBlackHole(new BlackHole("BH-1", new Vec2(0, 0), 120));
                Vec2 p = new Vec2(-620, -160);
                Vec2 v = new Vec2(420, 120);

                Particle probe = new Particle(p, v);
                probe.setColor(Color.rgb(255, 220, 140, 0.92));
                probe.setRadius(3.0);
                probe.setMaxTrailPoints(200);

                engine.addParticle(probe);
                engine.addRandomBurst(new Vec2(0, 0), 120, 420);
            }
            default -> { 
                engine.addBlackHole(new BlackHole("BH-1", new Vec2(0, 0), ui.getMass()));
                engine.addRandomBurst(new Vec2(0, 0), 420, 360);
            }
        }

        refreshBhSelect();
    }

    private void setupInputs() {
        canvas.setOnMouseMoved(e -> renderer.setMouse(e.getX(), e.getY(), true));
        canvas.setOnMouseExited(e -> renderer.setMouse(0, 0, false));

        canvas.setOnMouseClicked(e -> {
            if (e.getButton() != MouseButton.PRIMARY) return;

            Vec2 world = camera.screenToWorld(e.getX(), e.getY());

            if (ui.isPlaceBlackHoleMode()) {
                String id = engine.nextBlackHoleId();
                engine.addBlackHole(new BlackHole(id, world, 80));
                ui.setPlaceBlackHoleMode(false);
                refreshBhSelect();
                return;
            }

            Vec2 vel = engine.makeTangentialOrbitVelocity(world, 0.95);
            vel.x *= 0.95 + Math.random() * 0.12;
            vel.y *= 0.95 + Math.random() * 0.12;

            engine.addParticle(new Particle(world, vel));
        });

        canvas.setOnMousePressed(e -> {
            lastMouseX = e.getX();
            lastMouseY = e.getY();

            if (e.isShiftDown() && e.getButton() == MouseButton.PRIMARY) {
                BlackHole near = findBlackHoleNearScreen(e.getX(), e.getY(), 22);
                if (near != null) {
                    draggingBlackHole = near;
                    panning = false;
                    return;
                }
            }

            if (e.getButton() == MouseButton.MIDDLE || e.getButton() == MouseButton.SECONDARY) {
                panning = true;
            }
        });

        canvas.setOnMouseReleased(e -> {
            panning = false;
            draggingBlackHole = null;
        });

        canvas.setOnMouseDragged(e -> {
            double dx = e.getX() - lastMouseX;
            double dy = e.getY() - lastMouseY;

            if (draggingBlackHole != null) {
                Vec2 w = camera.screenToWorld(e.getX(), e.getY());
                draggingBlackHole.getPosition().x = w.x;
                draggingBlackHole.getPosition().y = w.y;
            } else if (panning) {
                camera.getCenterWorld().x -= dx / camera.getZoom();
                camera.getCenterWorld().y -= dy / camera.getZoom();
            }

            lastMouseX = e.getX();
            lastMouseY = e.getY();
        });

        canvas.addEventFilter(ScrollEvent.SCROLL, e -> {
            double delta = e.getDeltaY();
            if (Math.abs(delta) < 0.01) return;

            double factor = (delta > 0) ? 1.08 : 0.92;

            Vec2 before = camera.screenToWorld(e.getX(), e.getY());
            camera.setZoom(camera.getZoom() * factor);
            ui.setZoom(camera.getZoom());
            Vec2 after = camera.screenToWorld(e.getX(), e.getY());

            camera.getCenterWorld().x += (before.x - after.x);
            camera.getCenterWorld().y += (before.y - after.y);

            e.consume();
        });
    }

    private BlackHole findBlackHoleNearScreen(double sx, double sy, double radiusPx) {
        double best = Double.POSITIVE_INFINITY;
        BlackHole bestBh = null;

        for (BlackHole bh : engine.getBlackHoles()) {
            Vec2 c = camera.worldToScreen(bh.getPosition());
            double dx = c.x - sx;
            double dy = c.y - sy;
            double d2 = dx*dx + dy*dy;

            if (d2 < radiusPx * radiusPx && d2 < best) {
                best = d2;
                bestBh = bh;
            }
        }
        return bestBh;
    }

    private void setupLoop() {
        GraphicsContext g = canvas.getGraphicsContext2D();

        timer = new AnimationTimer() {
            private long lastNs = 0;

            @Override
            public void handle(long now) {
                if (lastNs == 0) {
                    lastNs = now;
                    return;
                }

                double dt = (now - lastNs) / 1_000_000_000.0;
                lastNs = now;

                dt = Math.min(dt, 0.05);

                renderer.setDrawTrails(ui.isTrailsEnabled());
                renderer.setDrawGridDistortion(ui.isGridDistortionEnabled());
                renderer.setDrawVelocityVectors(ui.isVelocityVectorsEnabled());

                if (!ui.isPaused()) {
                    double simDt = dt * ui.getSpeed();
                    engine.update(simDt, ui.isTrailsEnabled());
                }

                renderer.render(g, canvas.getWidth(), canvas.getHeight());
            }
        };
    }
}
