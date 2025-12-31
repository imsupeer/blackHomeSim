# Black Hole Simulator (JavaFX)

This project is an interactive, open-source 2D simulator, geared towards educational purposes, focusing on:

- Particle orbits (test particles)
- Event horizon (absorption)
- Numerical diagnostics (energy drift)
- Selectable physical models:
  - **Newtonian**
  - **Paczyński–Wiita** (pseudo-relativistic for Schwarzschild)

---

## Technologies

- Java 17+
- JavaFX (Canvas)
- Maven

---

## Runing

```bash
mvn clean javafx:run
```

---

## Controls (UI)

### Physics
- **Gravity model**
  - Newtonian
  - Paczynski–Wiita (pseudo-GR)
- **Integrator**
  - Velocity Verlet (fast+stable)
  - RK4 (precise)
  - RK45 adaptive (very precise)
- **Precision**
  - Adjusts RK4 substeps and RK45 tolerance
- **c (scale)**
  - Adjust the scale of the horizon radius (rs = 2GM/c²) to make it visible on the canvas.

### Simulation
- Speed
- Zoom
- Pause
- Trails / Grid distortion / Velocity vectors

### Entities
- BH selection (dropdown)
- Mass (selected)
- Place / Remove BH
- Add burst / Add particle / Clear

---

## HUD

- Shows the current model and integrator.
- Displays **Energy drift (%)**: if drift increases quickly, increase precision or reduce speed.
- At cursor:
  - |g| (field magnitude)
  - v_esc (approximate escape velocity)

---
