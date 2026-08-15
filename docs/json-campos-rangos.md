****** COMIENZO JSON REFERENCIA (float_input [1×22], orden metadata_backend.json)

Entrada ejemplo: Departamento, month=3, uso_horario_pico=si, horas_alto_consumo=6.5, cantidad_equipos=8

[[0.0, 1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 6.5, 8.0, 0.81249, 6.5, -2.83751]]

****** FIN JSON REFERENCIA

****** DESGLOSE

[
  0.0, 1.0, 0.0,                          # tipo_inmueble one-hot: Casa, Departamento, Monoambiente
  0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,  # month 1..12 (aquí mes 3)
  0.0, 1.0,                               # uso_horario_pico one-hot [no, si]:
                                          #   "no" → [1.0, 0.0]  |  "si" → [0.0, 1.0]
  6.5,                                    # horas_alto_consumo
  8.0,                                    # cantidad_equipos
  0.81249,                                # intensidad_por_equipo = horas / (equipos + 1e-5)
  6.5,                                    # horas_pico_interaccion (= horas si pico=si, else 0)
  -2.83751                                # desviacion_equipos_tipo = equipos - media(tipo)
]

****** FIN DESGLOSE

//////////////////

Variables Calculadas en Back-End (Synthetic Features)
A. intensidad_por_equipo
intensidad_por_equipo = horas_alto_consumo / (cantidad_equipos + 0.00001)
Nota: le sumamos 0.00001 al denominador por seguridad para evitar errores de división por cero si cantidad_equipos es 0.
B. horas_pico_interaccion
horas_pico_interaccion = uso_horario_pico.equalsIgnoreCase("si") ? horas_alto_consumo : 0.0
Multiplica por 1 si uso_horario_pico es "si", o fuerza 0.0 si es "no".
C. desviacion_equipos_tipo
desviacion_equipos_tipo = cantidad_equipos - media_segun_tipo_inmueble
Valores para media_segun_tipo_inmueble:

JSON
{
  "Casa": 18.290713952403173,
  "Departamento": 10.837505489679403,
  "Monoambiente": 4.9766260162601625
}
