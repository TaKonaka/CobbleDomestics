# CobbleDomestics

Mod de **Minecraft 1.21.1 / NeoForge 21.1.232** que añade un sistema de higiene para Pokémon de **Cobblemon ≥ 1.8.0**. El `modId` es `cobbledomestics`. El código vive en `src/main/java/cobbledomestics/` como proyecto NeoForge con `DeferredRegister` propio.

Dependencias declaradas en `META-INF/neoforge.mods.toml`:

| Módulo | Rango | Rol |
|---|---|---|
| `neoforge` | `[21.1.232,)` | Loader y event bus |
| `minecraft` | `[1.21.1]` | Runtime |
| `cobblemon` | `[1.8.0,)` | Entidades `Pokemon` / `PokemonEntity`, eventos de combate, aspectos |
| `kotlinforforge` | `[5.7.0,)` | Requerido por Cobblemon (API Kotlin) |

En Gradle, Cobblemon se declara como `compileOnly` (`com.cobblemon:neoforge:1.8.0+1.21.1`) para no duplicar el JAR en runtime (JarInJar / mixins). El JAR de Cobblemon y Kotlin for Forge se esperan en `run/mods`.

---

## Arquitectura

```
CobbleDomesticsMod          bootstrap: registros + networking
├── init/                   ítems y creative tabs (DeferredRegister)
├── item/                   SoapItem, ToallaItem (durabilidad 5, no stack)
├── particle/               SimpleParticleType soap_bubble
├── bath/                   dominio servidor (estado, NBT, comandos, handlers)
├── affection/              Humor / Confianza, Abrazo / Caricia, oferta de unión al equipo
└── client/                 partículas + overlay de suciedad + Interact Wheel + JoinOfferScreen
```

El constructor del `@Mod` registra:

1. `RegisterPayloadHandlersEvent` (canal de payloads bidireccional; el baño no usa mensajes custom).
2. `DeferredRegister` de ítems y particle types.
3. `BathHandler.registerCobblemonEvents()` — suscripción al event bus de Cobblemon (no al de NeoForge).
4. Registro de packets de affection / join offer.
Los handlers de baño, comandos y overlay cliente usan `@EventBusSubscriber(modid = "cobbledomestics")`.

### Sincronización cliente/servidor

El baño **no** usa `CustomPacketPayload`. El dato visual se replica con el sistema nativo de **forced aspects** de Cobblemon:

- Servidor: `Pokemon.setForcedAspects(...)` con `cobbledomestics-dirt-{0..5}`.
- Cliente: `PokemonEntity.getEntityData().get(PokemonEntity.getASPECTS())`.

El NBT persistente (`Pokemon.getPersistentData()`) es la fuente de verdad en servidor. Los aspectos son un canal de render.

---

## Modelo de datos (`BathData`)

Todo se guarda en un `CompoundTag` anidado:

```
Pokemon.persistentData
└── cobbledomestics          // ROOT
    ├── suciedad             // int [0, MAX_SUCIEDAD=5]
    ├── jabonoso             // int [0, MAX_JABONOSO=1]
    ├── mojado               // int 0 | 1
    ├── bath_state           // String name() de BathState
    └── last_dirt_battle     // UUID.toString() de la última batalla que ensució
```

`tag(Pokemon)` hace get-or-create del compound `cobbledomestics` sobre `getPersistentData()`. Los setters clampan y, cuando aplica, llaman a `syncDirtAspect` / `syncState`.

### Máquina de estados (`BathState`)

```
          suciedad > 0
NORMAL  ──────────────►  SUCIO
  ▲                        │
  │                        │ jabón  (jabonoso = MAX_JABONOSO)
  │                        ▼
  │                    ENJABONADO
  │                        │
  │                        │ cubo de agua  (jabonoso=0, mojado=1)
  │                        ▼
  │                     MOJADO
  │                        │
  └──── finishBath() ──────┘   toalla: suciedad=0, +amistad
```

`syncState(Pokemon)` es un reconciliador, no un transicionador libre. Reglas:

| Condición | Transición |
|---|---|
| `suciedad <= 0` y estado `SUCIO` | → `NORMAL` |
| `suciedad > 0`, `mojado == 0`, estado `NORMAL` | → `SUCIO` |
| `jabonoso >= 1`, estado `SUCIO` o `NORMAL`, `suciedad > 0` | → `ENJABONADO` |
| `mojado > 0` y estado `ENJABONADO` | → `MOJADO` |

`fromId` mapea el string NBT al enum; vacío o inválido → `NORMAL`.

### Aspectos de suciedad

Prefijo: `cobbledomestics-dirt-`.

`syncDirtAspect`:

1. Copia `pokemon.getForcedAspects()`.
2. Elimina cualquier aspect cuyo nombre empiece por el prefijo.
3. Si `suciedad > 0`, añade `cobbledomestics-dirt-{n}`.
4. Si `suciedad == 0` **y** (había aspect de suciedad **o** el tag `suciedad` existe), añade `cobbledomestics-dirt-0`. El `0` explícito permite al cliente distinguir “limpio conocido” de “sin dato”.
5. `setForcedAspects` solo si el set cambió.

`suciedadFromAspects` parsea el entero tras el prefijo; `NumberFormatException` → `-1` (sin dato).

---

## Origen de suciedad: combates Cobblemon

`BathHandler.registerCobblemonEvents()` se suscribe a:

- `CobblemonEvents.BATTLE_VICTORY`
- `CobblemonEvents.BATTLE_FLED`

Ambos delegan en `dirtyBattleParticipants(PokemonBattle)`.

Filtros por actor/Pokémon:

1. `BattleActor.getType() == ActorType.PLAYER` (salvajes/NPC no ensucian).
2. `BattlePokemon.getFacedOpponents()` no vacío (solo si combatió de verdad).
3. `getHealth() < getMaxHealth()` (sin daño recibido → no ensucia).
4. `pokemon.getOwnerUUID() != null`.
5. Deduplicación: `last_dirt_battle` igual al `battle.getBattleId()` → no-op.

Si pasa los filtros: `suciedad += 1` (cap 5) y se escribe el UUID de batalla. Aunque ya esté al máximo, se marca el UUID para no reintentar en el mismo combate.

Al spawnear un `PokemonEntity` (`EntityJoinLevelEvent`, solo servidor) se llama `syncDirtAspect` para reinyectar el overlay al mundo.

---

## Pipeline de interacción

### Entrada

`PlayerInteractEvent.EntityInteract` sobre un `PokemonEntity`. `tryBath` inspecciona el `ItemStack`:

| Ítem | Condición de registro | Efecto servidor |
|---|---|---|
| `cobbledomestics:soap` | `suciedad > 0` y estado ≠ `ENJABONADO`/`MOJADO` | `jabonoso=1`, estado `ENJABONADO`, `hurtAndBreak(SOAP_COST=1)` |
| `minecraft:water_bucket` | estado `ENJABONADO` | `jabonoso=0`, `mojado=1`, estado `MOJADO`, cubo → `BUCKET` (o drop si inventario lleno; creative no consume) |
| `cobbledomestics:toalla` | estado `MOJADO` | `finishBath` + `hurtAndBreak(1)` |

Si el resultado `consumesAction()`, el evento se cancela y se fija `CancellationResult` (evita que Cobblemon abra menús / montar / etc.).

En cliente, las tres acciones devuelven `SUCCESS` inmediatamente (animación de uso) y el servidor es autoritativo.

### Autorización

`canCareFor`: `pokemon.getOwnerUUID()` no nulo **y** igual a `player.getUUID()`. Si falla → `FAIL` + `message.cobbledomestics.bath.not_owner`.

### Anti-daño

Jabón y toalla son ítems “usables” sobre entidades. Para que el clic izquierdo no golpee:

- `AttackEntityEvent` (prioridad `HIGH`): cancela si el target es `PokemonEntity` y la mano principal es herramienta de baño.
- `LivingIncomingDamageEvent` (prioridad `HIGH`): cancela si la víctima es `PokemonEntity`, la fuente es `Player` y la mano principal es jabón/toalla.

El cubo de agua no entra en `isBathTool`; el cubo ya tiene su propio flujo de vanilla.

### Feedback

`playAround`: sonido en `SoundSource.PLAYERS` (0.8 / 1.1) + `ServerLevel.sendParticles` (16 partículas, offset 0.45/0.35/0.45, speed 0.02) en el centro vertical del AABB (`getY() + bbHeight * 0.5`).

| Paso | Sonido | Partícula |
|---|---|---|
| Jabón | `BUBBLE_COLUMN_UPWARDS_AMBIENT` | `cobbledomestics:soap_bubble` |
| Agua | `BUCKET_EMPTY` | `minecraft:falling_water` |
| Toalla | `WOOL_PLACE` | `minecraft:cloud` |

Mensajes: `player.displayClientMessage(..., true)` (action bar).

---

## Recompensas al terminar el baño (`finishBath`)

```
friendship = roll ∈ [FRIENDSHIP_ROLL_MIN=1, FRIENDSHIP_ROLL_MAX=3]  ×  suciedad
clearBath()          // suciedad=jabonoso=mojado=0, estado NORMAL, sync aspect
pokemon.incrementFriendship(friendship, true)   // si friendship > 0
```

La amistad escala linealmente con el nivel de suciedad acumulada **antes** de limpiar. Con suciedad 5 el rango es `[5, 15]`.

---

## Cliente: overlay de suciedad (`PokemonDirtLayer`)

Evento: `RenderLivingEvent.Pre` en `Dist.CLIENT`.

1. Entidad debe ser `PokemonEntity`.
2. Modelo del renderer debe ser `PosablePokemonEntityModel`.
3. `visibleSuciedad` > 0.
4. Se obtiene `PosableModel` y se inyecta un `ModelLayer` extra en `currentLayers` si aún no está.

Resolución de suciedad visible (primera fuente ≥ 0 gana):

1. Synced entity data: `PokemonEntity.getASPECTS()`.
2. `pokemon.getAspects()`.
3. Fallback NBT `BathData.getSuciedad` (solo útil si el cliente tiene el Pokémon hidratado).

El `ModelLayer` de Cobblemon no expone setters públicos para `name` / `texture` / `translucent`. Se construye por **reflexión** (`setAccessible`) una sola vez:

- `name` = `cobbledomestics_dirt`
- `texture` = `StaticModelTextureSupplier(cobbledomestics:textures/entity/pokemon/dirt.png)`
- `translucent` = `true`

Si la reflexión falla, `initFailed` corta reintentos y se loguea error.

Alpha por nivel (`ALPHA_BY_LEVEL`):

| suciedad | 0 | 1 | 2 | 3 | 4 | 5 |
|---|---|---|---|---|---|---|
| alpha | 0.00 | 0.28 | 0.44 | 0.60 | 0.78 | 0.95 |

Tint RGB = `(1,1,1)`; el nivel solo cambia el canal A. La capa se añade al final de `currentLayers` para dibujar encima del body. Si el layer ya está en la lista (mismo objeto o mismo `name`), no se duplica.

**Nota de ciclo de vida:** el layer se muta in-place (`overlay.getTint().set(...)`) y se reinyecta cada frame Pre-render. No hay `RenderLivingEvent.Post` que lo retire; Cobblemon reconstruye `currentLayers` en su propio pipeline de pose, así que el overlay no debería persistir a otras entidades, pero depende de que Cobblemon resetee las layers por modelo/pose.

### Partícula `soap_bubble`

- Registro: `DeferredRegister` → `SimpleParticleType(false)` (no override limiter).
- JSON: `assets/cobbledomestics/particles/soap_bubble.json` usa el sprite `minecraft:bubble`.
- Provider en `RegisterParticleProvidersEvent`.
- Física: tamaño 0.02, `hasPhysics=false`, `gravity=0`, lifetime `25 + rand(20)`, deriva vertical `+0.002`/tick y damping `0.85` en XYZ. Render: `PARTICLE_SHEET_OPAQUE`.

---

## Ítems

| Registro | Clase | Properties |
|---|---|---|
| `cobbledomestics:soap` | `SoapItem` | `durability(5)`, `stacksTo(1)` |
| `cobbledomestics:toalla` | `ToallaItem` | `durability(5)`, `stacksTo(1)` |

Ambos se inyectan en `CreativeModeTabs.TOOLS_AND_UTILITIES`. Creative (`instabuild`) no consume durabilidad ni el cubo.

---

## Comandos (permiso ≥ 2)

Raíz: `/cobbledomestics`

```
/cobbledomestics suciedad <valor:0..5>
/cobbledomestics estado
/cobbledomestics HumorReset
/cobbledomestics ConfianzaSet <valor:0..LvCaptura>
/cobbledomestics Animation <nombres…>
```

Target: Pokémon más cercano en un AABB proyectado 8 bloques en la dirección de mirada (`lookAngle * 8`, inflate 1). Si no hay hit, fallback al `PokemonEntity` que el jugador esté montando.

- `suciedad`: `setSuciedad`; si `valor > 0` fuerza `jabonoso=0`, `mojado=0`, estado `SUCIO`; si `0`, `clearBath`.
- `estado`: `syncState` y reporta `STATE | suciedad | jabonoso | mojado | humor | confianza`.
- `HumorReset`: pone humor en `DEFAULT_HUMOR` (10).
- `ConfianzaSet`: solo salvajes; fija confianza en `0..LvCaptura` (el parser acepta un rango amplio y `setConfianza` clampea al umbral del Pokémon).

---

## Networking

`CobbleDomesticsMod` registra payloads bidireccionales vía `addNetworkMessage` / `registerNetworking` (affection action, join offer / response). El baño no usa mensajes custom.

---

## Flujo end-to-end

```
Combate Cobblemon (PLAYER, enfrentó rival, HP < max)
        │
        ▼
persistentData.suciedad++  +  forcedAspect cobbledomestics-dirt-N
        │
        ▼
Cliente: RenderLivingEvent.Pre lee ASPECTS → overlay dirt.png @ alpha[N]
        │
Jugador dueño usa jabón → ENJABONADO + burbujas
        │
Cubo de agua → MOJADO + falling_water (cubo vacío)
        │
Toalla → friendship = rand(1..3)*N, aspect dirt-0
```

---

## Extensión

Puntos de anclaje naturales:

- **Más fuentes de suciedad:** llamar `BathData.addSuciedadFromBattle` o `setSuciedad` desde otros eventos Cobblemon/NeoForge (walk, weather, etc.). Respetar el cap 5 y, si se usa batalla, el UUID de dedupe.
- **Overlay de enjabonado/mojado:** el cliente solo lee suciedad vía aspects. `jabonoso`/`mojado` no se sincronizan a aspects; un overlay de espuma exigiría un aspect extra o un payload.

---

## Afecto: Humor, Confianza, Abrazo / Caricia

Paquete `cobbledomestics.affection` + hook cliente `AffectionClient`.

### Datos (`AffectionData`)

En el mismo compound `cobbledomestics` del NBT persistente:

| Key | Rango | Quién |
|---|---|---|
| `humor` | 0–10 (default 10 si ausente) | todos |
| `confianza` | 0–`LvCaptura` (default 0) | solo salvajes (`ownerUUID == null`) |

#### LvCaptura (umbral de domesticación)

El valor de confianza necesario para que un salvaje ofrezca unirse al equipo es dinámico por Pokémon:

```
LvCaptura = (LVPkm × Empacho) / 2
```

| Variable | Origen |
|---|---|
| `LVPkm` | `pokemon.getLevel()` |
| `Empacho` | `pokemon.getMaxFullness()` (saciedad máxima de Cobblemon) |
| `LvCaptura` | `max(1, (nivel × maxFullness) / 2)` — división entera |

API: `AffectionData.getLvCaptura(pokemon)`. `getConfianza` / `setConfianza` clampean a ese máximo.

Los salvajes **no usan Amistad** de Cobblemon: Caricia/Abrazo/Haba solo suman Confianza. La Amistad aplica a Pokémon capturados (dueño).

Cada **1200 ticks** (~1 min): `humor += 1` (cap 10) en party online y en `PokemonEntity` salvajes del mundo.

### Interact Wheel (Shift + clic derecho)

Igual que Cobblemon:

- **Dueño:** `CobblemonEvents.POKEMON_INTERACTION_GUI_CREATION` añade **Caricia** y **Abrazo** al wheel nativo.
- **Salvaje / no dueño:** el cliente abre un `InteractWheelGUI` solo con esas dos opciones (Cobblemon no envía el UI packet a no-dueños).

Iconos: `textures/gui/interact/caricia.png`, `abrazo.png`. Acción C2S: `AffectionActionPacket`.

| Acción | Humor | Extra | Salvaje | Capturado |
|---|---|---|---|---|
| Caricia | −3 | — | +rand(2–5) Confianza | +rand(2–5) Amistad |
| Abrazo | −4 | salvaje: Confianza > `LvCaptura / 2` | +rand(5–15) Confianza | +rand(5–15) Amistad |

Fallos: action bar + partículas `angry_villager`. Éxito: partículas `heart`. El baño **ignora** Shift para no chocar con el wheel.

Haba en salvaje: solo Confianza (sin Amistad ni texto de amistad en el action bar). Haba en dueño: Amistad.

### Unirse al equipo (confianza máxima)

Cuando un salvaje llega a **confianza ≥ `LvCaptura`** con la última Caricia/Abrazo/Haba (o ya está al máximo y se le vuelve a acariciar/abrazar/alimentar tras un rechazo), el servidor envía `JoinOfferPacket` y el cliente abre `JoinOfferScreen`:

- Mensaje: `%s quiere unirse a tu equipo`
- **Aceptar:** busca una `PokeBallItem` (mano principal → offhand → inventario), la asigna como `caughtBall`, añade con `party.add` (overflow a PC de Cobblemon) y despawnea la entidad. Si no hay ball: mensaje `join.no_ball` y se reabre la oferta.
- **Rechazar:** cierra la UI; la confianza se mantiene en `LvCaptura`. La próxima interacción exitosa vuelve a ofrecer.

Packets: S2C `join_offer`, C2S `join_offer_response`.
